package com.stockguardplus.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockguardplus.app.data.repository.OrganizationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// Backs the app-wide "can this account write real data" check. Browsing
// (including loading/viewing demo data) is free; creating or changing real
// data requires an active subscription — see StockGuardNavHost's
// requireSubscription() and the screens that call into it.
@HiltViewModel
class SubscriptionGateViewModel @Inject constructor(
    organizationRepository: OrganizationRepository
) : ViewModel() {
    val hasActiveAccess: StateFlow<Boolean> = organizationRepository.observeOrganization()
        .map { it?.hasActiveAccess == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
}
