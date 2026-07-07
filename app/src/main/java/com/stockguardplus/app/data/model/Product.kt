package com.stockguardplus.app.data.model

data class Product(
    val id: String,
    val name: String,
    val sku: String,
    val quantity: Int,
    val reorderPoint: Int,
    val category: String
) {
    val status: StockStatus
        get() = when {
            quantity <= 0 -> StockStatus.OUT_OF_STOCK
            quantity <= reorderPoint -> StockStatus.LOW_STOCK
            else -> StockStatus.IN_STOCK
        }
}
