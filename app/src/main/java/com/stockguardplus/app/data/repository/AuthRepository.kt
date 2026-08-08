package com.stockguardplus.app.data.repository

interface AuthRepository {
    val currentOrgId: String?

    val currentUserEmail: String?

    suspend fun signUp(email: String, password: String, businessName: String)

    suspend fun signIn(email: String, password: String)

    /** Signs in with a Google ID token from Credential Manager. Creates the
     * organization/member docs on this account's first sign-in, same as
     * [signUp] does for email/password. */
    suspend fun signInWithGoogle(idToken: String)

    fun signOut()

    suspend fun deleteAccount(password: String)
}
