package com.stockguardplus.app.ui.navigation

import androidx.lifecycle.ViewModel
import com.stockguardplus.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NavStartViewModel @Inject constructor(
    authRepository: AuthRepository
) : ViewModel() {
    val startDestination: String =
        if (authRepository.currentOrgId != null) Screen.Dashboard.route else Screen.Onboarding.route
}
