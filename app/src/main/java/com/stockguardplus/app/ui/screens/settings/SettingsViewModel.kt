package com.stockguardplus.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.stockguardplus.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    fun signOut() {
        authRepository.signOut()
    }
}
