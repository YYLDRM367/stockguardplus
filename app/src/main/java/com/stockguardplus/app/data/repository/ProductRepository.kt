package com.stockguardplus.app.data.repository

import com.stockguardplus.app.data.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun observeProducts(): Flow<List<Product>>

    suspend fun addProduct(product: Product)
}
