package com.stockguardplus.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.stockguardplus.app.data.local.LocalePreferences
import com.stockguardplus.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val localePreferences: LocalePreferences
) : ViewModel() {

    private val _languageTag = MutableStateFlow(localePreferences.languageTag)
    val languageTag: StateFlow<String?> = _languageTag.asStateFlow()

    fun setLanguage(tag: String?) {
        localePreferences.languageTag = tag
        _languageTag.value = tag
    }

    fun signOut() {
        authRepository.signOut()
    }
}
