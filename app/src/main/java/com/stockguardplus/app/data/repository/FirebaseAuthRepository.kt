package com.stockguardplus.app.data.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override val currentOrgId: String?
        get() = auth.currentUser?.uid

    override val currentUserEmail: String?
        get() = auth.currentUser?.email

    override suspend fun signUp(email: String, password: String, businessName: String) {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = requireNotNull(result.user?.uid) { "Sign-up succeeded but no user id was returned." }

        val orgRef = firestore.collection("organizations").document(uid)
        val memberRef = orgRef.collection("members").document(uid)

        val org = mapOf(
            "name" to businessName,
            "language" to Locale.getDefault().language,
            "subscriptionPlan" to "free",
            "subscriptionExpiry" to null
        )
        val member = mapOf("role" to "owner")

        firestore.runBatch { batch ->
            batch.set(orgRef, org)
            batch.set(memberRef, member)
        }.await()
    }

    override suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    override fun signOut() {
        auth.signOut()
    }

    override suspend fun deleteAccount(password: String) {
        val user = requireNotNull(auth.currentUser) { "No signed-in user." }
        val email = requireNotNull(user.email) { "This account has no email on file." }

        val credential = EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(credential).await()

        val orgId = user.uid
        val orgRef = firestore.collection("organizations").document(orgId)

        deleteCollection(orgRef.collection("products"))
        deleteCollection(orgRef.collection("categories"))
        deleteCollection(orgRef.collection("parties"))
        deleteCollection(orgRef.collection("orders"))
        deleteCollection(orgRef.collection("movements"))
        deleteCollection(orgRef.collection("members"))
        orgRef.delete().await()

        user.delete().await()
    }

    private suspend fun deleteCollection(collectionRef: CollectionReference) {
        val snapshot = collectionRef.get().await()
        if (snapshot.isEmpty) return
        snapshot.documents.chunked(450).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { document -> batch.delete(document.reference) }
            batch.commit().await()
        }
    }
}
