package com.stockguardplus.app.ui.screens.paywall

import android.app.Activity
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockguardplus.app.R
import com.stockguardplus.app.data.repository.BillingRepository
import com.stockguardplus.app.data.repository.OrganizationRepository
import com.stockguardplus.app.data.repository.PlanOffer
import com.stockguardplus.app.data.repository.PurchaseResult
import com.stockguardplus.app.data.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PaywallUiState {
    data object Loading : PaywallUiState
    data class Ready(val offers: List<PlanOffer>) : PaywallUiState
    data object Verifying : PaywallUiState
    data object Verified : PaywallUiState
    data class Error(@StringRes val messageRes: Int) : PaywallUiState
}

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val organizationRepository: OrganizationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PaywallUiState>(PaywallUiState.Loading)
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Already entitled (e.g. returning to this screen after sign-in,
            // or a cold start that always routes here first) — skip the
            // plan picker and let the caller move on to Dashboard.
            val organization = organizationRepository.observeOrganization().first()
            if (organization?.hasActiveAccess == true) {
                _uiState.value = PaywallUiState.Verified
            } else {
                loadOffers()
            }
        }
        viewModelScope.launch {
            billingRepository.purchaseResults.collect { result ->
                when (result) {
                    is PurchaseResult.Success -> verify(result.purchaseToken)
                    is PurchaseResult.Cancelled -> loadOffers()
                    is PurchaseResult.Error -> _uiState.value = PaywallUiState.Error(R.string.error_paywall_purchase_failed)
                }
            }
        }
    }

    fun loadOffers() {
        _uiState.value = PaywallUiState.Loading
        viewModelScope.launch {
            val offers = billingRepository.queryOffers()
            _uiState.value = if (offers.isEmpty()) {
                PaywallUiState.Error(R.string.error_paywall_load_failed)
            } else {
                PaywallUiState.Ready(offers)
            }
        }
    }

    fun selectPlan(activity: Activity, offer: PlanOffer) {
        billingRepository.launchPurchase(activity, offer)
    }

    private fun verify(purchaseToken: String) {
        _uiState.value = PaywallUiState.Verifying
        viewModelScope.launch {
            try {
                subscriptionRepository.verifyPurchase(purchaseToken)
                _uiState.value = PaywallUiState.Verified
            } catch (e: Exception) {
                _uiState.value = PaywallUiState.Error(R.string.error_paywall_verify_failed)
            }
        }
    }
}
