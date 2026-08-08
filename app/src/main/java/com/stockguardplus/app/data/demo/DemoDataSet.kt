package com.stockguardplus.app.data.demo

data class DemoCategory(val name: String, val sortOrder: Long)

data class DemoProduct(
    val name: String,
    val sku: String,
    val categoryIndex: Int,
    val reorderPoint: Int,
    val initialQuantity: Int,
    // How many days ago the demo "purchase" order for this product is dated —
    // spread out so Reports' date-range filters have something real to show,
    // not everything dumped on "today".
    val orderDaysAgo: Int
)

data class DemoParty(
    val name: String,
    val address: String,
    val phone1: String,
    val email: String
)

data class DemoDataSet(
    val categories: List<DemoCategory>,
    val products: List<DemoProduct>,
    val party: DemoParty
)

// Turkish demo set — a small hardware/warehouse business, matching how
// StockGuard+ is actually marketed and previously demoed (Elektrik/Su/
// Hırdavat). One product per category is seeded below its reorder point
// (or at zero) so the low-stock/out-of-stock alerts have something to show
// out of the box.
val DemoDataTr = DemoDataSet(
    categories = listOf(
        DemoCategory("Elektrik", 0),
        DemoCategory("Su Tesisatı", 1),
        DemoCategory("Hırdavat", 2),
        DemoCategory("Ofis Malzemeleri", 3)
    ),
    products = listOf(
        DemoProduct("LED Ampul 9W", "ELK-001", categoryIndex = 0, reorderPoint = 20, initialQuantity = 8, orderDaysAgo = 21),
        DemoProduct("Kablo 2.5mm (100m)", "ELK-002", categoryIndex = 0, reorderPoint = 5, initialQuantity = 12, orderDaysAgo = 14),
        DemoProduct("Priz Grubu", "ELK-003", categoryIndex = 0, reorderPoint = 10, initialQuantity = 0, orderDaysAgo = 3),
        DemoProduct("PVC Boru 1/2 inç", "SU-001", categoryIndex = 1, reorderPoint = 15, initialQuantity = 40, orderDaysAgo = 10),
        DemoProduct("Vana", "SU-002", categoryIndex = 1, reorderPoint = 8, initialQuantity = 20, orderDaysAgo = 5),
        DemoProduct("Çivi Kutusu 5cm", "HRD-001", categoryIndex = 2, reorderPoint = 10, initialQuantity = 25, orderDaysAgo = 7),
        DemoProduct("Cırt Bant", "HRD-002", categoryIndex = 2, reorderPoint = 5, initialQuantity = 3, orderDaysAgo = 1),
        DemoProduct("A4 Kağıt (Paket)", "OFS-001", categoryIndex = 3, reorderPoint = 10, initialQuantity = 30, orderDaysAgo = 18),
        DemoProduct("Dosya Klasörü", "OFS-002", categoryIndex = 3, reorderPoint = 5, initialQuantity = 15, orderDaysAgo = 2)
    ),
    party = DemoParty(
        name = "Yıldız Elektrik Toptan",
        address = "İstanbul, Türkiye",
        phone1 = "0212 555 00 00",
        email = "info@yildizelektrik.example"
    )
)

val DemoDataEn = DemoDataSet(
    categories = listOf(
        DemoCategory("Electrical", 0),
        DemoCategory("Plumbing", 1),
        DemoCategory("Hardware", 2),
        DemoCategory("Office Supplies", 3)
    ),
    products = listOf(
        DemoProduct("LED Bulb 9W", "ELK-001", categoryIndex = 0, reorderPoint = 20, initialQuantity = 8, orderDaysAgo = 21),
        DemoProduct("Cable 2.5mm (100m)", "ELK-002", categoryIndex = 0, reorderPoint = 5, initialQuantity = 12, orderDaysAgo = 14),
        DemoProduct("Outlet Set", "ELK-003", categoryIndex = 0, reorderPoint = 10, initialQuantity = 0, orderDaysAgo = 3),
        DemoProduct("PVC Pipe 1/2 in", "SU-001", categoryIndex = 1, reorderPoint = 15, initialQuantity = 40, orderDaysAgo = 10),
        DemoProduct("Valve", "SU-002", categoryIndex = 1, reorderPoint = 8, initialQuantity = 20, orderDaysAgo = 5),
        DemoProduct("Nail Box 5cm", "HRD-001", categoryIndex = 2, reorderPoint = 10, initialQuantity = 25, orderDaysAgo = 7),
        DemoProduct("Velcro Tape", "HRD-002", categoryIndex = 2, reorderPoint = 5, initialQuantity = 3, orderDaysAgo = 1),
        DemoProduct("A4 Paper (Pack)", "OFS-001", categoryIndex = 3, reorderPoint = 10, initialQuantity = 30, orderDaysAgo = 18),
        DemoProduct("Ring Binder", "OFS-002", categoryIndex = 3, reorderPoint = 5, initialQuantity = 15, orderDaysAgo = 2)
    ),
    party = DemoParty(
        name = "Northline Supply Co.",
        address = "Chicago, IL",
        phone1 = "+1 312 555 0100",
        email = "info@northlinesupply.example"
    )
)

// Only TR/EN exist for now — CLAUDE.md notes Spanish/French/German are
// planned but placeholder-only, so they fall back to English here too.
// Adding a language later is just one more branch + one more DemoDataSet.
fun demoDataSetFor(languageTag: String?): DemoDataSet =
    if (languageTag?.startsWith("tr") == true) DemoDataTr else DemoDataEn
