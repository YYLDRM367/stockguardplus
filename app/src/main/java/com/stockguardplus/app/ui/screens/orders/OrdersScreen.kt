package com.stockguardplus.app.ui.screens.orders

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockguardplus.app.R
import com.stockguardplus.app.data.model.Order
import com.stockguardplus.app.data.model.OrderStatus
import com.stockguardplus.app.data.model.OrderType
import com.stockguardplus.app.ui.theme.PaperBorder
import com.stockguardplus.app.ui.theme.PaperMuted
import com.stockguardplus.app.ui.theme.PaperSurface
import com.stockguardplus.app.ui.theme.StockGood
import com.stockguardplus.app.ui.theme.StockWarn
import java.text.DateFormat
import java.util.Locale

@Composable
fun OrdersScreen(
    onCreateOrder: (OrderType) -> Unit,
    onOrderClick: (String) -> Unit,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val purchaseOrders by viewModel.purchaseOrders.collectAsState()
    val salesOrders by viewModel.salesOrders.collectAsState()
    val companies by viewModel.companies.collectAsState()
    val companyNameById = companies.associate { it.id to it.name }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabType = if (selectedTab == 0) OrderType.PURCHASE else OrderType.SALE
    val orders = if (selectedTab == 0) purchaseOrders else salesOrders

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_orders)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onCreateOrder(tabType) }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add))
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.order_tab_purchase)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.order_tab_sale)) }
                )
            }

            if (orders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.orders_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(orders, key = { it.id }) { order ->
                        OrderRow(
                            order = order,
                            companyName = companyNameById[order.partyId],
                            onClick = { onOrderClick(order.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderRow(order: Order, companyName: String?, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(PaperSurface, MaterialTheme.shapes.small)
            .border(1.dp, PaperBorder, MaterialTheme.shapes.small)
            .padding(12.dp)
    ) {
        val dateFormatter = remember { DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()) }
        val formattedDate = order.date?.toDate()?.let { dateFormatter.format(it) }.orEmpty()

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = formattedDate, style = MaterialTheme.typography.bodyLarge)
            val (statusRes, statusColor) = if (order.orderStatus == OrderStatus.APPROVED) {
                R.string.status_approved to StockGood
            } else {
                R.string.status_draft to StockWarn
            }
            Text(text = stringResource(statusRes), style = MaterialTheme.typography.labelMedium, color = statusColor)
        }
        val reference = listOf(order.invoiceNumber, order.receiptNumber).filter { it.isNotBlank() }.joinToString(" · ")
        val secondaryLine = listOf(companyName.orEmpty(), reference).filter { it.isNotBlank() }.joinToString(" · ")
        Text(
            text = secondaryLine,
            style = MaterialTheme.typography.bodyMedium,
            color = PaperMuted,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
