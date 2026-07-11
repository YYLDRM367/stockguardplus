package com.stockguardplus.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.stockguardplus.app.data.model.Order
import com.stockguardplus.app.data.model.OrderLine
import com.stockguardplus.app.data.model.OrderStatus
import com.stockguardplus.app.data.model.OrderType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseOrderRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : OrderRepository {

    override fun observeOrders(type: OrderType): Flow<List<Order>> {
        val orgId = authRepository.currentOrgId ?: return flowOf(emptyList())

        return callbackFlow {
            val registration = firestore.collection("organizations")
                .document(orgId)
                .collection("orders")
                .whereEqualTo("type", type.value)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val orders = snapshot?.documents.orEmpty()
                        .mapNotNull { it.toObject(Order::class.java) }
                        .sortedByDescending { it.createdAt?.seconds ?: 0 }
                    trySend(orders)
                }
            awaitClose { registration.remove() }
        }
    }

    override fun observeOrder(orderId: String): Flow<Order?> {
        val orgId = authRepository.currentOrgId ?: return flowOf(null)

        return callbackFlow {
            val registration = firestore.collection("organizations")
                .document(orgId)
                .collection("orders")
                .document(orderId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    trySend(snapshot?.toObject(Order::class.java))
                }
            awaitClose { registration.remove() }
        }
    }

    override suspend fun createOrder(
        type: OrderType,
        orderNumber: String,
        partyId: String,
        lines: List<OrderLine>
    ): String {
        val orgId = requireNotNull(authRepository.currentOrgId) { "Cannot create an order while signed out." }

        val data = mapOf(
            "orderNumber" to orderNumber,
            "type" to type.value,
            "partyId" to partyId,
            "status" to OrderStatus.DRAFT.value,
            "lines" to lines.map { mapOf("productId" to it.productId, "quantity" to it.quantity) },
            "userId" to orgId,
            "createdAt" to FieldValue.serverTimestamp()
        )

        val reference = firestore.collection("organizations")
            .document(orgId)
            .collection("orders")
            .add(data)
            .await()

        return reference.id
    }

    override suspend fun approveOrder(orderId: String) {
        val orgId = requireNotNull(authRepository.currentOrgId) { "Cannot approve an order while signed out." }
        val orgRef = firestore.collection("organizations").document(orgId)
        val orderRef = orgRef.collection("orders").document(orderId)

        firestore.runTransaction { transaction ->
            val orderSnapshot = transaction.get(orderRef)
            val status = orderSnapshot.getString("status") ?: OrderStatus.DRAFT.value
            if (status != OrderStatus.DRAFT.value) {
                throw IllegalStateException("This order has already been approved.")
            }

            val type = OrderType.fromValue(orderSnapshot.getString("type") ?: OrderType.PURCHASE.value)
            val partyId = orderSnapshot.getString("partyId").orEmpty()

            @Suppress("UNCHECKED_CAST")
            val rawLines = orderSnapshot.get("lines") as? List<Map<String, Any>> ?: emptyList()
            val lines = rawLines.map {
                OrderLine(
                    productId = it["productId"] as? String ?: "",
                    quantity = (it["quantity"] as? Long)?.toInt() ?: 0
                )
            }

            val productRefs = lines.map { orgRef.collection("products").document(it.productId) }
            val productSnapshots = productRefs.map { transaction.get(it) }
            val movementRefs = lines.map { orgRef.collection("movements").document() }

            lines.forEachIndexed { index, line ->
                val currentQuantity = productSnapshots[index].getLong("quantity")?.toInt() ?: 0
                val newQuantity = when (type) {
                    OrderType.PURCHASE -> currentQuantity + line.quantity
                    OrderType.SALE -> {
                        if (line.quantity > currentQuantity) {
                            throw InsufficientStockException(
                                "Yetersiz stok (mevcut $currentQuantity adet, istenen ${line.quantity} adet)."
                            )
                        }
                        currentQuantity - line.quantity
                    }
                }

                transaction.update(productRefs[index], "quantity", newQuantity)
                transaction.set(
                    movementRefs[index],
                    mapOf(
                        "productId" to line.productId,
                        "type" to if (type == OrderType.PURCHASE) "in" else "out",
                        "quantity" to line.quantity,
                        "partyId" to partyId,
                        "orderId" to orderId,
                        "userId" to orgId,
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                )
            }

            transaction.update(
                orderRef,
                mapOf(
                    "status" to OrderStatus.APPROVED.value,
                    "approvedAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
    }

    override suspend fun deleteOrder(orderId: String) {
        val orgId = requireNotNull(authRepository.currentOrgId) { "Cannot delete an order while signed out." }

        firestore.collection("organizations")
            .document(orgId)
            .collection("orders")
            .document(orderId)
            .delete()
            .await()
    }
}
