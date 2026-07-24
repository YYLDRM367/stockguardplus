package com.stockguardplus.app.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** null = follow system, otherwise "light" or "dark". */
@Singleton
class ThemePreferences @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME_MODE, null))
    val themeMode: StateFlow<String?> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String?) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
