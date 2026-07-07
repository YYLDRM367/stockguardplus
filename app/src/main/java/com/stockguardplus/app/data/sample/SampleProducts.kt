package com.stockguardplus.app.data.sample

import com.stockguardplus.app.data.model.Product

// Placeholder in-memory data, standing in for the Firestore repository until it's wired up.
object SampleProducts {
    val list = listOf(
        Product(id = "1", name = "Vidalı Somun 8mm", sku = "WH-1042", quantity = 128, reorderPoint = 40, category = "Bağlantı Elemanları"),
        Product(id = "2", name = "Kablo Bağı 200mm", sku = "WH-2210", quantity = 34, reorderPoint = 40, category = "Elektrik"),
        Product(id = "3", name = "Ambalaj Kutusu L", sku = "WH-3305", quantity = 0, reorderPoint = 20, category = "Ambalaj"),
        Product(id = "4", name = "Etiket Rulosu", sku = "WH-1187", quantity = 76, reorderPoint = 30, category = "Sarf Malzeme")
    )
}
