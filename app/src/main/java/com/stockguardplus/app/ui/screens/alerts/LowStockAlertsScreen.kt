package com.stockguardplus.app.ui.screens.alerts

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockguardplus.app.R
import com.stockguardplus.app.data.model.Product
import com.stockguardplus.app.ui.components.StockStatusChip
import com.stockguardplus.app.ui.theme.PaperBorder
import com.stockguardplus.app.ui.theme.PaperMuted
import com.stockguardplus.app.ui.theme.PaperSurface

@Composable
fun LowStockAlertsScreen(
    filter: AlertFilter,
    onProductClick: (String) -> Unit,
    viewModel: AlertsViewModel = hiltViewModel()
) {
    LaunchedEffect(filter) { viewModel.setFilter(filter) }

    val products by viewModel.alertProducts.collectAsState()

    val titleRes = when (filter) {
        AlertFilter.LOW_STOCK -> R.string.stat_low_stock
        AlertFilter.OUT_OF_STOCK -> R.string.stat_out_of_stock
        AlertFilter.ALL -> R.string.tab_alerts
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(titleRes)) }) }
    ) { innerPadding ->
        if (products.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.alerts_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    AlertRow(product = product, onClick = { onProductClick(product.id) })
                }
            }
        }
    }
}

@Composable
private fun AlertRow(product: Product, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(PaperSurface, MaterialTheme.shapes.small)
            .border(1.5.dp, PaperBorder, MaterialTheme.shapes.small)
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = product.name, style = MaterialTheme.typography.bodyLarge)
            StockStatusChip(status = product.status)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = product.sku, style = MaterialTheme.typography.bodyMedium, color = PaperMuted)
            Text(
                text = "${product.quantity} / ${product.reorderPoint}",
                style = MaterialTheme.typography.bodyMedium,
                color = PaperMuted
            )
        }
    }
}
