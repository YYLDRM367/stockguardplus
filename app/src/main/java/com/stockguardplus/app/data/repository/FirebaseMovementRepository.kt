package com.stockguardplus.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.stockguardplus.app.data.model.Movement
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

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

    override fun observeMovementsInRange(startMillis: Long, endMillis: Long): Flow<List<Movement>> {
        val orgId = authRepository.currentOrgId ?: return flowOf(emptyList())

        return callbackFlow {
            val registration = firestore.collection("organizations")
                .document(orgId)
                .collection("movements")
                .whereGreaterThanOrEqualTo("timestamp", Timestamp(startMillis / 1000, 0))
                .whereLessThanOrEqualTo("timestamp", Timestamp(endMillis / 1000, 0))
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val movements = snapshot?.documents.orEmpty()
                        .mapNotNull { it.toObject(Movement::class.java) }
                        .sortedByDescending { it.timestamp?.seconds ?: 0 }
                    trySend(movements)
                }
            awaitClose { registration.remove() }
        }
    }
}
