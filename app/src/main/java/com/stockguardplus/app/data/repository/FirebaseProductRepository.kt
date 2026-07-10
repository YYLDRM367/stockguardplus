package com.stockguardplus.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.stockguardplus.app.data.model.Product
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseProductRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : ProductRepository {

    override fun observeProducts(): Flow<List<Product>> {
        val orgId = authRepository.currentOrgId ?: return flowOf(emptyList())

        return callbackFlow {
            val registration = firestore.collection("organizations")
                .document(orgId)
                .collection("products")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val products = snapshot?.documents.orEmpty().mapNotNull { it.toObject(Product::class.java) }
                    trySend(products)
                }
            awaitClose { registration.remove() }
        }
    }

    override suspend fun addProduct(product: Product) {
        val orgId = requireNotNull(authRepository.currentOrgId) { "Cannot add a product while signed out." }

        val data = mapOf(
            "name" to product.name,
            "sku" to product.sku,
            "quantity" to product.quantity,
            "reorderPoint" to product.reorderPoint,
            "category" to product.category
        )

        firestore.collection("organizations")
            .document(orgId)
            .collection("products")
            .add(data)
            .await()
    }
}
