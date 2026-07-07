package com.stockguardplus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.stockguardplus.app.ui.navigation.StockGuardNavHost
import com.stockguardplus.app.ui.theme.StockGuardPlusTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StockGuardPlusTheme {
                StockGuardNavHost()
            }
        }
    }
}
