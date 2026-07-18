package com.stockguardplus.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockguardplus.app.R
import com.stockguardplus.app.data.model.Movement
import com.stockguardplus.app.data.model.MovementType
import com.stockguardplus.app.data.model.StockStatus
import com.stockguardplus.app.ui.components.StatCard
import com.stockguardplus.app.ui.theme.PaperBorder
import com.stockguardplus.app.ui.theme.PaperMuted
import com.stockguardplus.app.ui.theme.PaperSurface
import com.stockguardplus.app.ui.theme.StockBad
import com.stockguardplus.app.ui.theme.StockGood
import com.stockguardplus.app.ui.theme.StockWarn
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    onProductsClick: () -> Unit,
    onAlertsClick: () -> Unit,
    onProductClick: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val products by viewModel.products.collectAsState()
    val recentMovements by viewModel.recentMovements.collectAsState()
    val companies by viewModel.companies.collectAsState()
    val lowStockCount = products.count { it.status == StockStatus.LOW_STOCK }
    val outOfStockCount = products.count { it.status == StockStatus.OUT_OF_STOCK }

    val productNameById = remember(products) { products.associate { it.id to it.name } }
    val companyNameById = remember(companies) { companies.associate { it.id to it.name } }

    val dateFormatter = remember { DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault()) }
    val timeFormatter = remember { DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()) }
    val now = remember { Date() }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_dashboard)) }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatCard(
                    value = products.size.toString(),
                    label = stringResource(R.string.stat_total_products),
                    modifier = Modifier.weight(1f).clickable(onClick = onProductsClick)
                )
                StatCard(
                    value = lowStockCount.toString(),
                    label = stringResource(R.string.stat_low_stock),
                    valueColor = StockWarn,
                    modifier = Modifier.weight(1f).clickable(onClick = onAlertsClick)
                )
                StatCard(
                    value = outOfStockCount.toString(),
                    label = stringResource(R.string.stat_out_of_stock),
                    valueColor = StockBad,
                    modifier = Modifier.weight(1f).clickable(onClick = onAlertsClick)
                )
            }

            Column {
                Text(text = dateFormatter.format(now), style = MaterialTheme.typography.titleMedium)
                Text(text = timeFormatter.format(now), style = MaterialTheme.typography.bodyMedium, color = PaperMuted)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.dashboard_recent_activity), style = MaterialTheme.typography.titleSmall)

                if (recentMovements.isEmpty()) {
                    Text(
                        text = stringResource(R.string.movements_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = PaperMuted
                    )
                } else {
                    recentMovements.forEach { movement ->
                        RecentMovementRow(
                            movement = movement,
                            productName = productNameById[movement.productId].orEmpty(),
                            companyName = companyNameById[movement.partyId].orEmpty(),
                            dateFormatter = dateFormatter,
                            onClick = { onProductClick(movement.productId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentMovementRow(
    movement: Movement,
    productName: String,
    companyName: String,
    dateFormatter: DateFormat,
    onClick: () -> Unit
) {
    val isIn = movement.movementType == MovementType.IN
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(PaperSurface, MaterialTheme.shapes.small)
            .border(1.dp, PaperBorder, MaterialTheme.shapes.small)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = productName, style = MaterialTheme.typography.bodyLarge)
            val date = movement.timestamp?.toDate()?.let { dateFormatter.format(it) }.orEmpty()
            val subtitle = listOf(date, companyName).filter { it.isNotBlank() }.joinToString(" · ")
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = PaperMuted)
        }
        Text(
            text = if (isIn) "+${movement.quantity}" else "-${movement.quantity}",
            style = MaterialTheme.typography.bodyLarge,
            color = if (isIn) StockGood else StockBad
        )
    }
}
