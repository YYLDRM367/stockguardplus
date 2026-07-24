package com.stockguardplus.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockguardplus.app.data.local.LocalePreferences
import com.stockguardplus.app.data.local.ThemePreferences
import com.stockguardplus.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DeleteAccountState {
    data object Idle : DeleteAccountState
    data object InProgress : DeleteAccountState
    data object Success : DeleteAccountState
    data object Error : DeleteAccountState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val localePreferences: LocalePreferences,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    val currentUserEmail: String? = authRepository.currentUserEmail

    private val _languageTag = MutableStateFlow(localePreferences.languageTag)
    val languageTag: StateFlow<String?> = _languageTag.asStateFlow()

    val themeMode: StateFlow<String?> = themePreferences.themeMode

    private val _deleteAccountState = MutableStateFlow<DeleteAccountState>(DeleteAccountState.Idle)
    val deleteAccountState: StateFlow<DeleteAccountState> = _deleteAccountState.asStateFlow()

    fun setLanguage(tag: String?) {
        localePreferences.languageTag = tag
        _languageTag.value = tag
    }

    fun setThemeMode(mode: String?) {
        themePreferences.setThemeMode(mode)
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun deleteAccount(password: String) {
        _deleteAccountState.value = DeleteAccountState.InProgress
        viewModelScope.launch {
            try {
                authRepository.deleteAccount(password)
                _deleteAccountState.value = DeleteAccountState.Success
            } catch (e: Exception) {
                _deleteAccountState.value = DeleteAccountState.Error
            }
        }
    }

    fun resetDeleteAccountState() {
        _deleteAccountState.value = DeleteAccountState.Idle
    }
}
