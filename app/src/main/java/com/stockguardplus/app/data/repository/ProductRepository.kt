package com.stockguardplus.app.data.repository

import com.stockguardplus.app.data.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun observeProducts(): Flow<List<Product>>

    fun observeProduct(productId: String): Flow<Product?>

    suspend fun findProductByBarcode(barcode: String): Product?

    suspend fun addProduct(product: Product)

    suspend fun updateProductDetails(
        productId: String,
        name: String,
        sku: String,
        barcode: String,
        reorderPoint: Int,
        categoryId: String
    )

    suspend fun updateQuantity(productId: String, quantity: Int)

    suspend fun deleteProduct(productId: String)
}
