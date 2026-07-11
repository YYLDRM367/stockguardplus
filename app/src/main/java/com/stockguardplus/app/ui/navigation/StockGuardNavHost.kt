package com.stockguardplus.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockguardplus.app.R
import com.stockguardplus.app.ui.screens.alerts.LowStockAlertsScreen
import com.stockguardplus.app.ui.screens.categories.CategoriesScreen
import com.stockguardplus.app.ui.screens.companies.CompaniesScreen
import com.stockguardplus.app.ui.screens.dashboard.DashboardScreen
import com.stockguardplus.app.ui.screens.onboarding.OnboardingScreen
import com.stockguardplus.app.ui.screens.products.AddEditProductScreen
import com.stockguardplus.app.ui.screens.products.ProductDetailScreen
import com.stockguardplus.app.ui.screens.products.ProductListScreen
import com.stockguardplus.app.ui.screens.scanner.BarcodeLookupScreen
import com.stockguardplus.app.ui.screens.scanner.BarcodeScannerScreen
import com.stockguardplus.app.ui.screens.settings.SettingsScreen

private data class BottomTab(val screen: Screen, val icon: ImageVector, val labelRes: Int)

private val bottomTabs = listOf(
    BottomTab(Screen.Dashboard, Icons.Filled.Dashboard, R.string.tab_dashboard),
    BottomTab(Screen.Products, Icons.Filled.Inventory2, R.string.tab_products),
    BottomTab(Screen.Alerts, Icons.Filled.NotificationsActive, R.string.tab_alerts),
    BottomTab(Screen.Settings, Icons.Filled.Settings, R.string.tab_settings)
)

@Composable
fun StockGuardNavHost(navStartViewModel: NavStartViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val showBottomBar = bottomTabs.any { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.screen.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.route == tab.screen.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = navStartViewModel.startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onSignedIn = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen()
            }
            composable(Screen.Products.route) {
                ProductListScreen(
                    onProductClick = { id -> navController.navigate(Screen.ProductDetail.createRoute(id)) },
                    onAddProduct = { navController.navigate(Screen.AddEditProduct.createRoute()) },
                    onScanBarcode = { navController.navigate(Screen.ScanBarcode.createRoute(ScanMode.LOOKUP)) }
                )
            }
            composable(
                route = Screen.ProductDetail.route,
                arguments = listOf(navArgument("productId") { type = NavType.StringType })
            ) { entry ->
                val productId = entry.arguments?.getString("productId").orEmpty()
                ProductDetailScreen(
                    productId = productId,
                    onDeleted = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Screen.AddEditProduct.createRoute(productId = id)) }
                )
            }
            composable(
                route = Screen.AddEditProduct.route,
                arguments = listOf(
                    navArgument("productId") {
                        type = NavType.StringType
                        nullable = true
                    },
                    navArgument("barcode") {
                        type = NavType.StringType
                        nullable = true
                    }
                )
            ) { entry ->
                val productId = entry.arguments?.getString("productId")
                val initialBarcode = entry.arguments?.getString("barcode")?.let { decodeRouteParam(it) }
                val scannedBarcode by entry.savedStateHandle
                    .getStateFlow<String?>("scanned_barcode", null)
                    .collectAsState()
                AddEditProductScreen(
                    productId = productId,
                    initialBarcode = initialBarcode,
                    scannedBarcode = scannedBarcode,
                    onScanBarcode = { navController.navigate(Screen.ScanBarcode.createRoute(ScanMode.FIELD)) },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.ScanBarcode.route,
                arguments = listOf(navArgument("mode") { type = NavType.StringType })
            ) { entry ->
                val mode = ScanMode.fromValue(entry.arguments?.getString("mode"))
                BarcodeScannerScreen(
                    onBarcodeScanned = { value ->
                        when (mode) {
                            ScanMode.FIELD -> {
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("scanned_barcode", value)
                                navController.popBackStack()
                            }
                            ScanMode.LOOKUP -> {
                                navController.navigate(Screen.BarcodeLookup.createRoute(value)) {
                                    popUpTo(Screen.Products.route)
                                }
                            }
                        }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.BarcodeLookup.route,
                arguments = listOf(navArgument("barcode") { type = NavType.StringType })
            ) { entry ->
                val barcode = decodeRouteParam(entry.arguments?.getString("barcode").orEmpty())
                BarcodeLookupScreen(
                    barcode = barcode,
                    onFound = { productId ->
                        navController.navigate(Screen.ProductDetail.createRoute(productId)) {
                            popUpTo(Screen.Products.route)
                        }
                    },
                    onAddProductWithBarcode = { code ->
                        navController.navigate(Screen.AddEditProduct.createRoute(barcode = code)) {
                            popUpTo(Screen.Products.route)
                        }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(Screen.Categories.route) {
                CategoriesScreen()
            }
            composable(Screen.Companies.route) {
                CompaniesScreen()
            }
            composable(Screen.Alerts.route) {
                LowStockAlertsScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onManageCategories = { navController.navigate(Screen.Categories.route) },
                    onManageCompanies = { navController.navigate(Screen.Companies.route) },
                    onSignedOut = {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
