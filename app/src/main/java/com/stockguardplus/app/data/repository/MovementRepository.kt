package com.stockguardplus.app.data.repository

import com.stockguardplus.app.data.model.Movement
import com.stockguardplus.app.data.model.MovementType
import kotlinx.coroutines.flow.Flow

interface MovementRepository {
    fun observeMovements(productId: String): Flow<List<Movement>>

    suspend fun recordMovement(productId: String, type: MovementType, quantity: Int, partyId: String)
}
