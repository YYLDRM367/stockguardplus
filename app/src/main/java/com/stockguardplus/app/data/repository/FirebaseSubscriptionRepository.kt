package com.stockguardplus.app.data.repository

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSubscriptionRepository @Inject constructor(
    private val functions: FirebaseFunctions
) : SubscriptionRepository {

    override suspend fun verifyPurchase(purchaseToken: String) {
        functions.getHttpsCallable("verifyPurchase")
            .call(mapOf("purchaseToken" to purchaseToken))
            .await()
    }
}
