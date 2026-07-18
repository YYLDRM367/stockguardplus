package com.stockguardplus.app.ui.navigation

import com.stockguardplus.app.data.model.OrderType
import com.stockguardplus.app.ui.screens.alerts.AlertFilter
import java.net.URLDecoder
import java.net.URLEncoder

private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
fun decodeRouteParam(value: String) = URLDecoder.decode(value, "UTF-8")

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Dashboard : Screen("dashboard")
    data object Products : Screen("products")
    data object Categories : Screen("categories")
    data object Companies : Screen("companies")
    data object Orders : Screen("orders")
    data object Reports : Screen("reports")
    data object Alerts : Screen("alerts?filter={filter}") {
        fun createRoute(filter: AlertFilter = AlertFilter.ALL) = "alerts?filter=${filter.name}"
    }
    data object Settings : Screen("settings")

    data object CreateOrder : Screen("orders/create/{type}") {
        fun createRoute(type: OrderType) = "orders/create/${type.value}"
    }

    data object OrderDetail : Screen("orders/{orderId}") {
        fun createRoute(orderId: String) = "orders/${encode(orderId)}"
    }

    data object ProductDetail : Screen("products/{productId}") {
        fun createRoute(productId: String) = "products/$productId"
    }

    data object AddEditProduct : Screen("products/edit?productId={productId}&barcode={barcode}") {
        fun createRoute(productId: String? = null, barcode: String? = null): String {
            val params = mutableListOf<String>()
            if (productId != null) params += "productId=${encode(productId)}"
            if (barcode != null) params += "barcode=${encode(barcode)}"
            return if (params.isEmpty()) "products/edit" else "products/edit?${params.joinToString("&")}"
        }
    }

    data object ScanBarcode : Screen("scan/{mode}") {
        fun createRoute(mode: ScanMode) = "scan/${mode.value}"
    }

    data object BarcodeLookup : Screen("scan/lookup-result/{barcode}") {
        fun createRoute(barcode: String) = "scan/lookup-result/${encode(barcode)}"
    }
}

enum class ScanMode(val value: String) {
    FIELD("field"),
    LOOKUP("lookup");

    companion object {
        fun fromValue(value: String?) = entries.find { it.value == value } ?: FIELD
    }
}
