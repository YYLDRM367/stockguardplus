package com.stockguardplus.app.data.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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
        createOrganization(uid, businessName)
    }

    override suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    override suspend fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val user = requireNotNull(result.user) { "Google sign-in succeeded but no user was returned." }

        // Google Sign-In doesn't collect a business name up front like the
        // email sign-up form does, so a brand-new account falls back to the
        // Google account's display name — there's no rename UI yet, but the
        // Firestore field itself isn't otherwise special.
        if (result.additionalUserInfo?.isNewUser == true) {
            createOrganization(user.uid, user.displayName ?: user.email.orEmpty())
        }
    }

    // Subscription fields are deliberately absent here — there's no free
    // tier, so an org has no entitlement at all until verifyPurchase
    // (Cloud Function) writes subscriptionStatus after a real purchase.
    private suspend fun createOrganization(uid: String, businessName: String) {
        val orgRef = firestore.collection("organizations").document(uid)
        val memberRef = orgRef.collection("members").document(uid)

        val org = mapOf(
            "name" to businessName,
            "language" to Locale.getDefault().language
        )
        val member = mapOf("role" to "owner")

        firestore.runBatch { batch ->
            batch.set(orgRef, org)
            batch.set(memberRef, member)
        }.await()
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
