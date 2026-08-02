package com.stockguardplus.app.data.repository

interface SubscriptionRepository {
    /** Calls the verifyPurchase Cloud Function, which re-checks the Play
     * Developer API and writes the organization's subscription fields. */
    suspend fun verifyPurchase(purchaseToken: String)
}
