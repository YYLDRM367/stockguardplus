package com.stockguardplus.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.stockguardplus.app.data.model.Organization
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseOrganizationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : OrganizationRepository {

    override fun observeOrganization(): Flow<Organization?> {
        val orgId = authRepository.currentOrgId ?: return flowOf(null)

        return callbackFlow {
            val registration = firestore.collection("organizations")
                .document(orgId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    trySend(snapshot?.toObject(Organization::class.java))
                }
            awaitClose { registration.remove() }
        }
    }

    override suspend fun markDemoDataOffered() {
        val orgId = requireNotNull(authRepository.currentOrgId) { "Cannot update organization while signed out." }

        firestore.collection("organizations")
            .document(orgId)
            .update("demoDataOffered", true)
            .await()
    }
}
