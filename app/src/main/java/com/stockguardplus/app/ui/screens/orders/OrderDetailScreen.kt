package com.stockguardplus.app.ui.screens.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockguardplus.app.R
import com.stockguardplus.app.data.model.OrderLine
import com.stockguardplus.app.data.model.OrderStatus
import com.stockguardplus.app.ui.theme.PaperBorder
import com.stockguardplus.app.ui.theme.PaperMuted
import com.stockguardplus.app.ui.theme.PaperSurface
import com.stockguardplus.app.ui.theme.StockBad
import com.stockguardplus.app.ui.theme.StockGood
import com.stockguardplus.app.ui.theme.StockWarn
import java.text.DateFormat
import java.util.Locale

@Composable
fun OrderDetailScreen(
    orderId: String,
    onDeleted: () -> Unit,
    viewModel: OrderDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(orderId) { viewModel.load(orderId) }

    val order by viewModel.order.collectAsState()
    val companies by viewModel.companies.collectAsState()
    val products by viewModel.products.collectAsState()
    val isDeleted by viewModel.isDeleted.collectAsState()
    val approveError by viewModel.approveError.collectAsState()
    val isApproving by viewModel.isApproving.collectAsState()

    var showApproveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isDeleted) {
        if (isDeleted) onDeleted()
    }

    val dateFormatter = remember { DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()) }
    val formattedDate = order?.date?.toDate()?.let { dateFormatter.format(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(formattedDate ?: stringResource(R.string.screen_order_detail)) },
                actions = {
                    if (order?.orderStatus == OrderStatus.DRAFT) {
                        IconButton(onClick = { showApproveDialog = true }) {
                            Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.action_approve_order))
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        val current = order
        if (current == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.order_not_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val companyName = companies.find { it.id == current.partyId }?.name.orEmpty()
            val productNameById = products.associate { it.id to it.name }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = companyName, style = MaterialTheme.typography.bodyLarge)
                        val (statusRes, statusColor) = if (current.orderStatus == OrderStatus.APPROVED) {
                            R.string.status_approved to StockGood
                        } else {
                            R.string.status_draft to StockWarn
                        }
                        Text(
                            text = stringResource(statusRes),
                            style = MaterialTheme.typography.labelLarge,
                            color = statusColor
                        )
                    }
                }

                val reference = listOf(current.invoiceNumber, current.receiptNumber)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                if (reference.isNotBlank()) {
                    item {
                        Text(text = reference, style = MaterialTheme.typography.bodyMedium, color = PaperMuted)
                    }
                }

                approveError?.let { message ->
                    item {
                        Text(text = message, color = StockBad, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.field_order_lines),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(current.lines) { line ->
                    OrderLineRowReadOnly(line = line, productName = productNameById[line.productId].orEmpty())
                }
            }
        }
    }

    if (showApproveDialog) {
        AlertDialog(
            onDismissRequest = { showApproveDialog = false },
            text = { Text(stringResource(R.string.confirm_approve_order)) },
            confirmButton = {
                TextButton(
                    enabled = !isApproving,
                    onClick = {
                        showApproveDialog = false
                        viewModel.approve()
                    }
                ) {
                    Text(stringResource(R.string.action_approve_order))
                }
            },
            dismissButton = {
                TextButton(onClick = { showApproveDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            text = { Text(stringResource(R.string.confirm_delete_order)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.delete()
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun OrderLineRowReadOnly(line: OrderLine, productName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PaperSurface, MaterialTheme.shapes.small)
            .border(1.dp, PaperBorder, MaterialTheme.shapes.small)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = productName, style = MaterialTheme.typography.bodyLarge)
        Text(text = line.quantity.toString(), style = MaterialTheme.typography.bodyLarge, color = PaperMuted)
    }
}
