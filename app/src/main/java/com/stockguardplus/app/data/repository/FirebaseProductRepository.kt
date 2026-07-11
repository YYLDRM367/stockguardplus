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

    override fun observeProduct(productId: String): Flow<Product?> {
        val orgId = authRepository.currentOrgId ?: return flowOf(null)

        return callbackFlow {
            val registration = firestore.collection("organizations")
                .document(orgId)
                .collection("products")
                .document(productId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    trySend(snapshot?.toObject(Product::class.java))
                }
            awaitClose { registration.remove() }
        }
    }

    override suspend fun findProductByBarcode(barcode: String): Product? {
        val orgId = authRepository.currentOrgId ?: return null

        val snapshot = firestore.collection("organizations")
            .document(orgId)
            .collection("products")
            .whereEqualTo("barcode", barcode)
            .limit(1)
            .get()
            .await()

        return snapshot.documents.firstOrNull()?.toObject(Product::class.java)
    }

    override suspend fun updateProductDetails(
        productId: String,
        name: String,
        sku: String,
        barcode: String,
        reorderPoint: Int,
        categoryId: String
    ) {
        val orgId = requireNotNull(authRepository.currentOrgId) { "Cannot update a product while signed out." }

        val data = mapOf(
            "name" to name,
            "sku" to sku,
            "barcode" to barcode,
            "reorderPoint" to reorderPoint,
            "categoryId" to categoryId
        )

        firestore.collection("organizations")
            .document(orgId)
            .collection("products")
            .document(productId)
            .update(data)
            .await()
    }

    override suspend fun updateQuantity(productId: String, quantity: Int) {
        val orgId = requireNotNull(authRepository.currentOrgId) { "Cannot update a product while signed out." }

        firestore.collection("organizations")
            .document(orgId)
            .collection("products")
            .document(productId)
            .update("quantity", quantity)
            .await()
    }

    override suspend fun deleteProduct(productId: String) {
        val orgId = requireNotNull(authRepository.currentOrgId) { "Cannot delete a product while signed out." }

        firestore.collection("organizations")
            .document(orgId)
            .collection("products")
            .document(productId)
            .delete()
            .await()
    }

    override suspend fun addProduct(product: Product) {
        val orgId = requireNotNull(authRepository.currentOrgId) { "Cannot add a product while signed out." }

        val data = mapOf(
            "name" to product.name,
            "sku" to product.sku,
            "barcode" to product.barcode,
            "quantity" to product.quantity,
            "reorderPoint" to product.reorderPoint,
            "categoryId" to product.categoryId
        )

        firestore.collection("organizations")
            .document(orgId)
            .collection("products")
            .add(data)
            .await()
    }
}
