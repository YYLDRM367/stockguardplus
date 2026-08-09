package com.stockguardplus.app.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class Category(
    @DocumentId val id: String = "",
    val name: String = "",
    val sortOrder: Long = 0,
    @get:PropertyName("isDemo")
    val isDemo: Boolean = false
)
