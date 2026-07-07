package com.stockguardplus.app.ui.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Dashboard : Screen("dashboard")
    data object Products : Screen("products")
    data object Categories : Screen("categories")
    data object Alerts : Screen("alerts")
    data object Settings : Screen("settings")

    data object ProductDetail : Screen("products/{productId}") {
        fun createRoute(productId: String) = "products/$productId"
    }

    data object AddEditProduct : Screen("products/edit?productId={productId}") {
        fun createRoute(productId: String? = null) =
            if (productId != null) "products/edit?productId=$productId" else "products/edit"
    }
}
