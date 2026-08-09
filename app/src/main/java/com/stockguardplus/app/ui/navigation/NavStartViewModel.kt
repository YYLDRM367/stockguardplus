package com.stockguardplus.app.ui.navigation

import androidx.lifecycle.ViewModel
import com.stockguardplus.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NavStartViewModel @Inject constructor(
    authRepository: AuthRepository
) : ViewModel() {
    // Browsing (including demo data) is free, so signed-in users land on
    // Dashboard directly — no entitlement check needed here. Paywall is
    // only reached when a write action requires it (see
    // StockGuardNavHost.requireSubscription) or from Settings.
    val startDestination: String =
        if (authRepository.currentOrgId != null) Screen.Dashboard.route else Screen.Onboarding.route
}
