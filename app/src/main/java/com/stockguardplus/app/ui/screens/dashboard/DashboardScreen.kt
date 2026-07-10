package com.stockguardplus.app.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockguardplus.app.R
import com.stockguardplus.app.data.model.StockStatus
import com.stockguardplus.app.ui.components.StatCard
import com.stockguardplus.app.ui.theme.StockBad
import com.stockguardplus.app.ui.theme.StockWarn

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val products by viewModel.products.collectAsState()
    val lowStockCount = products.count { it.status == StockStatus.LOW_STOCK }
    val outOfStockCount = products.count { it.status == StockStatus.OUT_OF_STOCK }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_dashboard)) }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatCard(
                    value = products.size.toString(),
                    label = stringResource(R.string.stat_total_products),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = lowStockCount.toString(),
                    label = stringResource(R.string.stat_low_stock),
                    valueColor = StockWarn,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = outOfStockCount.toString(),
                    label = stringResource(R.string.stat_out_of_stock),
                    valueColor = StockBad,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
