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
import com.stockguardplus.app.R
import com.stockguardplus.app.ui.screens.alerts.LowStockAlertsScreen
import com.stockguardplus.app.ui.screens.categories.CategoriesScreen
import com.stockguardplus.app.ui.screens.dashboard.DashboardScreen
import com.stockguardplus.app.ui.screens.onboarding.OnboardingScreen
import com.stockguardplus.app.ui.screens.products.AddEditProductScreen
import com.stockguardplus.app.ui.screens.products.ProductDetailScreen
import com.stockguardplus.app.ui.screens.products.ProductListScreen
import com.stockguardplus.app.ui.screens.settings.SettingsScreen

private data class BottomTab(val screen: Screen, val icon: ImageVector, val labelRes: Int)

private val bottomTabs = listOf(
    BottomTab(Screen.Dashboard, Icons.Filled.Dashboard, R.string.tab_dashboard),
    BottomTab(Screen.Products, Icons.Filled.Inventory2, R.string.tab_products),
    BottomTab(Screen.Alerts, Icons.Filled.NotificationsActive, R.string.tab_alerts),
    BottomTab(Screen.Settings, Icons.Filled.Settings, R.string.tab_settings)
)

@Composable
fun StockGuardNavHost() {
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
            startDestination = Screen.Onboarding.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onContinue = {
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
                    onAddProduct = { navController.navigate(Screen.AddEditProduct.createRoute()) }
                )
            }
            composable(
                route = Screen.ProductDetail.route,
                arguments = listOf(navArgument("productId") { type = NavType.StringType })
            ) { entry ->
                val productId = entry.arguments?.getString("productId").orEmpty()
                ProductDetailScreen(productId = productId)
            }
            composable(
                route = Screen.AddEditProduct.route,
                arguments = listOf(
                    navArgument("productId") {
                        type = NavType.StringType
                        nullable = true
                    }
                )
            ) { entry ->
                val productId = entry.arguments?.getString("productId")
                AddEditProductScreen(productId = productId)
            }
            composable(Screen.Categories.route) {
                CategoriesScreen()
            }
            composable(Screen.Alerts.route) {
                LowStockAlertsScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onManageCategories = { navController.navigate(Screen.Categories.route) })
            }
        }
    }
}
