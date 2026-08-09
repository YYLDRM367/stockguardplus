package com.stockguardplus.app.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class Company(
    @DocumentId val id: String = "",
    val name: String = "",
    val address: String = "",
    val phone1: String = "",
    val phone2: String = "",
    val email: String = "",
    @get:PropertyName("isDemo")
    val isDemo: Boolean = false
)
