package com.stockguardplus.app.data.repository

import com.stockguardplus.app.data.model.Movement
import kotlinx.coroutines.flow.Flow

interface MovementRepository {
    fun observeMovements(productId: String): Flow<List<Movement>>

    fun observeMovementsInRange(startMillis: Long, endMillis: Long): Flow<List<Movement>>

    fun observeRecentMovements(limit: Int): Flow<List<Movement>>
}
