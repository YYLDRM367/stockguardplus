package com.stockguardplus.app.data.repository

import com.stockguardplus.app.data.model.Order
import com.stockguardplus.app.data.model.OrderLine
import com.stockguardplus.app.data.model.OrderType
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun observeOrders(type: OrderType): Flow<List<Order>>

    fun observeOrder(orderId: String): Flow<Order?>

    suspend fun createOrder(type: OrderType, orderNumber: String, partyId: String, lines: List<OrderLine>): String

    suspend fun approveOrder(orderId: String)

    suspend fun deleteOrder(orderId: String)
}
