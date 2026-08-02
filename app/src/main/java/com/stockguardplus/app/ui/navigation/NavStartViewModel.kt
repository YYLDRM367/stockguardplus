package com.stockguardplus.app.ui.navigation

import androidx.lifecycle.ViewModel
import com.stockguardplus.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NavStartViewModel @Inject constructor(
    authRepository: AuthRepository
) : ViewModel() {
    // Signed-in users always land on Paywall first — it checks entitlement
    // itself and bounces straight to Dashboard if the org already has an
    // active subscription/trial, so this stays a cheap synchronous check
    // instead of needing an async Firestore read before the first frame.
    val startDestination: String =
        if (authRepository.currentOrgId != null) Screen.Paywall.route else Screen.Onboarding.route
}
