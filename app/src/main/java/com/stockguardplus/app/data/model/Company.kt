package com.stockguardplus.app.data.model

import com.google.firebase.firestore.DocumentId

data class Company(
    @DocumentId val id: String = "",
    val name: String = "",
    val address: String = "",
    val phone1: String = "",
    val phone2: String = "",
    val email: String = ""
)
