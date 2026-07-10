package com.stockguardplus.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.stockguardplus.app.data.model.Category
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseCategoryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : CategoryRepository {

    override fun observeCategories(): Flow<List<Category>> {
        val orgId = authRepository.currentOrgId ?: return flowOf(emptyList())

        return callbackFlow {
            val registration = firestore.collection("organizations")
                .document(orgId)
                .collection("categories")
                .orderBy("sortOrder", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val categories = snapshot?.documents.orEmpty().mapNotNull { it.toObject(Category::class.java) }
                    trySend(categories)
                }
            awaitClose { registration.remove() }
        }
    }

    override suspend fun addCategory(name: String): String {
        val orgId = requireNotNull(authRepository.currentOrgId) { "Cannot add a category while signed out." }

        val data = mapOf(
            "name" to name,
            "sortOrder" to System.currentTimeMillis()
        )

        val reference = firestore.collection("organizations")
            .document(orgId)
            .collection("categories")
            .add(data)
            .await()

        return reference.id
    }

    override suspend fun renameCategory(categoryId: String, name: String) {
        val orgId = requireNotNull(authRepository.currentOrgId) { "Cannot rename a category while signed out." }

        firestore.collection("organizations")
            .document(orgId)
            .collection("categories")
            .document(categoryId)
            .update("name", name)
            .await()
    }

    override suspend fun deleteCategory(categoryId: String) {
        val orgId = requireNotNull(authRepository.currentOrgId) { "Cannot delete a category while signed out." }
        val orgRef = firestore.collection("organizations").document(orgId)

        val affectedProducts = orgRef.collection("products")
            .whereEqualTo("categoryId", categoryId)
            .get()
            .await()

        firestore.runBatch { batch ->
            affectedProducts.documents.forEach { document ->
                batch.update(document.reference, "categoryId", "")
            }
            batch.delete(orgRef.collection("categories").document(categoryId))
        }.await()
    }
}
