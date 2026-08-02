package com.stockguardplus.app.data.repository

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Period
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val PRODUCT_ID = "stockguardplus_premium83"

@Singleton
class PlayBillingRepository @Inject constructor(
    @ApplicationContext context: Context
) : BillingRepository {

    private val _purchaseResults = MutableSharedFlow<PurchaseResult>(extraBufferCapacity = 1)
    override val purchaseResults: Flow<PurchaseResult> = _purchaseResults

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val purchase = purchases?.firstOrNull()
                if (purchase != null) {
                    acknowledgeIfNeeded(purchase)
                    _purchaseResults.tryEmit(PurchaseResult.Success(purchase.purchaseToken))
                } else {
                    _purchaseResults.tryEmit(PurchaseResult.Error("No purchase returned."))
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED ->
                _purchaseResults.tryEmit(PurchaseResult.Cancelled)
            else ->
                _purchaseResults.tryEmit(PurchaseResult.Error(billingResult.debugMessage))
        }
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().build())
        .build()

    private suspend fun ensureConnected() {
        if (billingClient.isReady) return
        suspendCancellableCoroutine { cont ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (cont.isActive) cont.resume(Unit)
                }
                override fun onBillingServiceDisconnected() {
                    // Next call to ensureConnected() will retry.
                }
            })
        }
    }

    override suspend fun queryOffers(): List<PlanOffer> {
        ensureConnected()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        return suspendCancellableCoroutine { cont ->
            billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    if (cont.isActive) cont.resume(emptyList())
                    return@queryProductDetailsAsync
                }
                val productDetails = productDetailsList.firstOrNull()
                if (productDetails == null) {
                    if (cont.isActive) cont.resume(emptyList())
                    return@queryProductDetailsAsync
                }
                val offers = productDetails.subscriptionOfferDetails.orEmpty().mapNotNull { offerDetails ->
                    val plan = SubscriptionPlan.fromBasePlanId(offerDetails.basePlanId) ?: return@mapNotNull null
                    val phases = offerDetails.pricingPhases.pricingPhaseList
                    val paidPhase = phases.lastOrNull() ?: return@mapNotNull null
                    val trialDays = phases.firstOrNull { it.priceAmountMicros == 0L }
                        ?.billingPeriod
                        ?.let { runCatching { Period.parse(it).days }.getOrNull() }
                    PlanOffer(
                        plan = plan,
                        offerToken = offerDetails.offerToken,
                        formattedPrice = paidPhase.formattedPrice,
                        billingPeriodIso8601 = paidPhase.billingPeriod,
                        freeTrialDays = trialDays,
                        productDetails = productDetails
                    )
                }
                if (cont.isActive) cont.resume(offers)
            }
        }
    }

    override fun launchPurchase(activity: Activity, offer: PlanOffer) {
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(offer.productDetails)
            .setOfferToken(offer.offerToken)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    override suspend fun queryExistingPurchaseToken(): String? {
        ensureConnected()
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        return suspendCancellableCoroutine { cont ->
            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    if (cont.isActive) cont.resume(null)
                    return@queryPurchasesAsync
                }
                val purchase = purchases.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                if (purchase != null) acknowledgeIfNeeded(purchase)
                if (cont.isActive) cont.resume(purchase?.purchaseToken)
            }
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { /* Cloud Function is the source of truth for entitlement. */ }
        }
    }
}
