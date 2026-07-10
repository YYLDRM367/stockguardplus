package com.stockguardplus.app.data.repository

interface AuthRepository {
    val currentOrgId: String?

    suspend fun signUp(email: String, password: String, businessName: String)

    suspend fun signIn(email: String, password: String)

    fun signOut()
}
