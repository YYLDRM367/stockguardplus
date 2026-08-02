package com.stockguardplus.app.ui.screens.paywall

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockguardplus.app.R
import com.stockguardplus.app.data.repository.PlanOffer
import com.stockguardplus.app.data.repository.SubscriptionPlan
import com.stockguardplus.app.ui.theme.PaperAccent
import com.stockguardplus.app.ui.theme.PaperBorder
import com.stockguardplus.app.ui.theme.PaperMuted
import com.stockguardplus.app.ui.theme.PaperSurface
import com.stockguardplus.app.ui.theme.StockBad
import com.stockguardplus.app.ui.theme.StockGood

@Composable
fun PaywallScreen(
    onSubscribed: () -> Unit,
    viewModel: PaywallViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalContext.current as Activity

    LaunchedEffect(uiState) {
        if (uiState is PaywallUiState.Verified) onSubscribed()
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.screen_paywall)) }) }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is PaywallUiState.Loading, is PaywallUiState.Verifying -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        if (state is PaywallUiState.Verifying) {
                            Text(
                                text = stringResource(R.string.paywall_verifying),
                                color = PaperMuted,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
                is PaywallUiState.Verified -> Unit
                is PaywallUiState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stringResource(state.messageRes), color = StockBad)
                        TextButton(onClick = { viewModel.loadOffers() }) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
                is PaywallUiState.Ready -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.paywall_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = PaperMuted
                        )
                        state.offers
                            .sortedBy { planOrder(it.plan) }
                            .forEach { offer ->
                                PlanCard(offer = offer, onSelect = { viewModel.selectPlan(activity, offer) })
                            }
                    }
                }
            }
        }
    }
}

private fun planOrder(plan: SubscriptionPlan) = when (plan) {
    SubscriptionPlan.MONTHLY -> 0
    SubscriptionPlan.QUARTERLY -> 1
    SubscriptionPlan.YEARLY -> 2
}

private fun planLabelRes(plan: SubscriptionPlan) = when (plan) {
    SubscriptionPlan.MONTHLY -> R.string.paywall_plan_monthly
    SubscriptionPlan.QUARTERLY -> R.string.paywall_plan_quarterly
    SubscriptionPlan.YEARLY -> R.string.paywall_plan_yearly
}

@Composable
private fun PlanCard(offer: PlanOffer, onSelect: () -> Unit) {
    val highlighted = offer.plan == SubscriptionPlan.YEARLY
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .background(PaperSurface, MaterialTheme.shapes.medium)
            .border(if (highlighted) 2.dp else 1.5.dp, if (highlighted) PaperAccent else PaperBorder, MaterialTheme.shapes.medium)
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(planLabelRes(offer.plan)), style = MaterialTheme.typography.titleMedium)
            Text(text = offer.formattedPrice, style = MaterialTheme.typography.titleMedium)
        }
        offer.freeTrialDays?.let { days ->
            Text(
                text = stringResource(R.string.paywall_trial_badge, days),
                style = MaterialTheme.typography.labelMedium,
                color = StockGood,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        TextButton(onClick = onSelect, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.paywall_select_plan))
        }
    }
}
