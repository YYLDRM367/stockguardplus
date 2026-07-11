package com.stockguardplus.app.data.repository

import com.stockguardplus.app.data.model.Movement
import kotlinx.coroutines.flow.Flow

interface MovementRepository {
    fun observeMovements(productId: String): Flow<List<Movement>>
}
