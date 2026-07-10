package com.stockguardplus.app.data.repository

import com.google.firebase.auth.FirebaseAuth
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
}
