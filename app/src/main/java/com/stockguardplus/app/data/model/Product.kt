package com.stockguardplus.app.data.model

import com.google.firebase.firestore.DocumentId

data class Product(
    @DocumentId val id: String = "",
    val name: String = "",
    val sku: String = "",
    val quantity: Int = 0,
    val reorderPoint: Int = 0,
    val categoryId: String = ""
) {
    val status: StockStatus
        get() = when {
            quantity <= 0 -> StockStatus.OUT_OF_STOCK
            quantity <= reorderPoint -> StockStatus.LOW_STOCK
            else -> StockStatus.IN_STOCK
        }
}
