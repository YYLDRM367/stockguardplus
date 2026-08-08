package com.stockguardplus.app.data.repository

interface DemoDataRepository {
    /** Seeds demo categories/products/company/purchase orders (approved, so
     * quantities and movement history are real) in the current org's
     * language, then marks the org so this isn't offered again. */
    suspend fun seedDemoData()

    /** Deletes every record flagged isDemo=true across products, categories,
     * parties, orders, and movements — never touches user-entered data. */
    suspend fun clearDemoData()
}
