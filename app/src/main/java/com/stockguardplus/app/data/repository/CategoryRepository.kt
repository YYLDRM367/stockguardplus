package com.stockguardplus.app.data.repository

import com.stockguardplus.app.data.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>

    suspend fun addCategory(name: String): String

    suspend fun renameCategory(categoryId: String, name: String)

    suspend fun deleteCategory(categoryId: String)
}
