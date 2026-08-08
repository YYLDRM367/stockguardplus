package com.stockguardplus.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

enum class SubscriptionStatus(val value: String) {
    TRIAL("trial"),
    ACTIVE("active"),
    GRACE_PERIOD("grace_period"),
    EXPIRED("expired"),
    CANCELED("canceled");

    companion object {
        fun fromValue(value: String?) = entries.find { it.value == value }
    }
}

data class Organization(
    @DocumentId val id: String = "",
    val name: String = "",
    val language: String = "",
    val subscriptionStatus: String? = null,
    val subscriptionPlan: String? = null,
    val subscriptionExpiry: Timestamp? = null,
    val demoDataOffered: Boolean = false
) {
    val status: SubscriptionStatus? get() = SubscriptionStatus.fromValue(subscriptionStatus)

    // trial/active/grace_period all mean "let the user in" — grace_period is
    // Play's own retry window after a failed renewal payment, access stays
    // on so the user isn't locked out mid-retry.
    val hasActiveAccess: Boolean
        get() = status == SubscriptionStatus.TRIAL ||
            status == SubscriptionStatus.ACTIVE ||
            status == SubscriptionStatus.GRACE_PERIOD
}
