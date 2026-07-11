package com.stockguardplus.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.stockguardplus.app.data.model.Company
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseCompanyRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : CompanyRepository {

    override fun observeCompanies(): Flow<List<Company>> {
        val orgId = authRepository.currentOrgId ?: return flowOf(emptyList())

        return callbackFlow {
            val registration = firestore.collection("organizations")
                .document(orgId)
                .collection("parties")
                .orderBy("name", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val companies = snapshot?.documents.orEmpty().mapNotNull { it.toObject(Company::class.java) }
                    trySend(companies)
                }
            awaitClose { registration.remove() }
        }
    }

    override suspend fun addCompany(company: Company): String {
        val orgId = requireNotNull(authRepository.currentOrgId) { "Cannot add a company while signed out." }

        val data = mapOf(
            "name" to company.name,
            "address" to company.address,
            "phone1" to company.phone1,
            "phone2" to company.phone2,
            "email" to company.email
        )

        val reference = firestore.collection("organizations")
            .document(orgId)
            .collection("parties")
            .add(data)
            .await()

        return reference.id
    }

    override suspend fun deleteCompany(companyId: String) {
        val orgId = requireNotNull(authRepository.currentOrgId) { "Cannot delete a company while signed out." }

        firestore.collection("organizations")
            .document(orgId)
            .collection("parties")
            .document(companyId)
            .delete()
            .await()
    }
}
