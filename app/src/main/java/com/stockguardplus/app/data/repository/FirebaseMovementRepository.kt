package com.stockguardplus.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.stockguardplus.app.data.model.Movement
import com.stockguardplus.app.data.model.MovementType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

class InsufficientStockException(message: String) : Exception(message)

@Singleton
class FirebaseMovementRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : MovementRepository {

    override fun observeMovements(productId: String): Flow<List<Movement>> {
        val orgId = authRepository.currentOrgId ?: return flowOf(emptyList())

        return callbackFlow {
            val registration = firestore.collection("organizations")
                .document(orgId)
                .collection("movements")
                .whereEqualTo("productId", productId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val movements = snapshot?.documents.orEmpty()
                        .mapNotNull { it.toObject(Movement::class.java) }
                        .sortedByDescending { it.timestamp?.seconds ?: 0 }
                        .take(20)
                    trySend(movements)
                }
            awaitClose { registration.remove() }
        }
    }

    override suspend fun recordMovement(productId: String, type: MovementType, quantity: Int, partyId: String) {
        val orgId = requireNotNull(authRepository.currentOrgId) { "Cannot record a movement while signed out." }

        val productRef = firestore.collection("organizations")
            .document(orgId)
            .collection("products")
            .document(productId)
        val movementRef = firestore.collection("organizations")
            .document(orgId)
            .collection("movements")
            .document()

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(productRef)
            val currentQuantity = snapshot.getLong("quantity")?.toInt() ?: 0

            val newQuantity = when (type) {
                MovementType.IN -> currentQuantity + quantity
                MovementType.OUT -> {
                    if (quantity > currentQuantity) {
                        throw InsufficientStockException("Yetersiz stok: mevcut $currentQuantity adet.")
                    }
                    currentQuantity - quantity
                }
            }

            transaction.update(productRef, "quantity", newQuantity)
            transaction.set(
                movementRef,
                mapOf(
                    "productId" to productId,
                    "type" to if (type == MovementType.IN) "in" else "out",
                    "quantity" to quantity,
                    "partyId" to partyId,
                    "userId" to orgId,
                    "timestamp" to FieldValue.serverTimestamp()
                )
            )
        }.await()
    }
}
