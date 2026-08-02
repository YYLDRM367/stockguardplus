package com.stockguardplus.app.data.repository

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.Flow

enum class SubscriptionPlan(val basePlanId: String) {
    MONTHLY("monthly"),
    QUARTERLY("quarterly"),
    YEARLY("yearly");

    companion object {
        fun fromBasePlanId(value: String?) = entries.find { it.basePlanId == value }
    }
}

data class PlanOffer(
    val plan: SubscriptionPlan,
    val offerToken: String,
    val formattedPrice: String,
    val billingPeriodIso8601: String,
    val freeTrialDays: Int?,
    val productDetails: ProductDetails
)

sealed interface PurchaseResult {
    data object Cancelled : PurchaseResult
    data class Success(val purchaseToken: String) : PurchaseResult
    data class Error(val message: String) : PurchaseResult
}

interface BillingRepository {
    suspend fun queryOffers(): List<PlanOffer>

    fun launchPurchase(activity: Activity, offer: PlanOffer)

    val purchaseResults: Flow<PurchaseResult>

    /** Re-checks Play for an existing subscription purchase, e.g. on app start. */
    suspend fun queryExistingPurchaseToken(): String?
}
