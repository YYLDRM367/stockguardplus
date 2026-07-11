package com.stockguardplus.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

enum class MovementType {
    IN, OUT
}

data class Movement(
    @DocumentId val id: String = "",
    val productId: String = "",
    val type: String = "",
    val quantity: Int = 0,
    val partyId: String = "",
    val userId: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null
) {
    val movementType: MovementType
        get() = if (type == "out") MovementType.OUT else MovementType.IN
}
