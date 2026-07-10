package com.stockguardplus.app.data.model

import com.google.firebase.firestore.DocumentId

data class Category(
    @DocumentId val id: String = "",
    val name: String = "",
    val sortOrder: Long = 0
)
