package com.stockguardplus.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.stockguardplus.app.data.local.LocalePreferences
import com.stockguardplus.app.ui.navigation.StockGuardNavHost
import com.stockguardplus.app.ui.theme.StockGuardPlusTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val languageTag = LocalePreferences.readLanguageTag(newBase)
        if (languageTag.isNullOrBlank()) {
            super.attachBaseContext(newBase)
            return
        }
        val locale = Locale.forLanguageTag(languageTag)
        val config = newBase.resources.configuration
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

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
