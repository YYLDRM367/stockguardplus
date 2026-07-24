package com.stockguardplus.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.stockguardplus.app.data.local.LocalePreferences
import com.stockguardplus.app.data.local.ThemePreferences
import com.stockguardplus.app.ui.navigation.StockGuardNavHost
import com.stockguardplus.app.ui.theme.StockGuardPlusTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var themePreferences: ThemePreferences

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
            val themeMode by themePreferences.themeMode.collectAsState()
            StockGuardPlusTheme(themeMode = themeMode) {
                StockGuardNavHost()
            }
        }
    }
}
