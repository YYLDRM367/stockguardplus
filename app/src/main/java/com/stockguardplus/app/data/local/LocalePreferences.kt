package com.stockguardplus.app.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalePreferences @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences("locale_prefs", Context.MODE_PRIVATE)

    var languageTag: String?
        get() = prefs.getString(KEY_LANGUAGE_TAG, null)
        set(value) {
            prefs.edit().putString(KEY_LANGUAGE_TAG, value).apply()
        }

    companion object {
        private const val KEY_LANGUAGE_TAG = "language_tag"

        fun readLanguageTag(context: Context): String? =
            context.getSharedPreferences("locale_prefs", Context.MODE_PRIVATE)
                .getString(KEY_LANGUAGE_TAG, null)
    }
}
