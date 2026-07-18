package com.stockguardplus.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

enum class OrderType(val value: String) {
    PURCHASE("purchase"),
    SALE("sale");

    companion object {
        fun fromValue(value: String) = entries.find { it.value == value } ?: PURCHASE
    }
}

enum class OrderStatus(val value: String) {
    DRAFT("draft"),
    APPROVED("approved");

    companion object {
        fun fromValue(value: String) = entries.find { it.value == value } ?: DRAFT
    }
}

data class OrderLine(
    val productId: String = "",
    val quantity: Int = 0
)

data class Order(
    @DocumentId val id: String = "",
    val date: Timestamp? = null,
    val invoiceNumber: String = "",
    val receiptNumber: String = "",
    val type: String = "",
    val partyId: String = "",
    val status: String = "draft",
    val lines: List<OrderLine> = emptyList(),
    val userId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    val approvedAt: Timestamp? = null
) {
    val orderType: OrderType get() = OrderType.fromValue(type)
    val orderStatus: OrderStatus get() = OrderStatus.fromValue(status)
}
