package com.stockguardplus.app.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class Product(
    @DocumentId val id: String = "",
    val name: String = "",
    val sku: String = "",
    val barcode: String = "",
    val quantity: Int = 0,
    val reorderPoint: Int = 0,
    val categoryId: String = "",
    // Without this, Firestore's reflection-based deserializer sees Kotlin's
    // generated isDemo() getter and looks for a field named "demo" instead
    // of "isDemo" — silently defaulting every read back to false even
    // though the document field is genuinely true.
    @get:PropertyName("isDemo")
    val isDemo: Boolean = false
) {
    val status: StockStatus
        get() = when {
            quantity <= 0 -> StockStatus.OUT_OF_STOCK
            quantity <= reorderPoint -> StockStatus.LOW_STOCK
            else -> StockStatus.IN_STOCK
        }
}
