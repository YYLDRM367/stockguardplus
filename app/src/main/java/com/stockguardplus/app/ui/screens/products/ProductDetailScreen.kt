package com.stockguardplus.app.ui.screens.products

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.stockguardplus.app.data.model.Movement
import com.stockguardplus.app.data.model.MovementType
import com.stockguardplus.app.ui.components.StockStatusChip
import com.stockguardplus.app.ui.theme.PaperBorder
import com.stockguardplus.app.ui.theme.PaperMuted
import com.stockguardplus.app.ui.theme.PaperSurface
import com.stockguardplus.app.ui.theme.StockBad
import com.stockguardplus.app.ui.theme.StockGood

@Composable
fun ProductDetailScreen(
    productId: String,
    onDeleted: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(productId) { viewModel.load(productId) }

    val product by viewModel.product.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val companies by viewModel.companies.collectAsState()
    val movements by viewModel.movements.collectAsState()
    val isDeleted by viewModel.isDeleted.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isDeleted) {
        if (isDeleted) onDeleted()
    }

    val uncategorizedLabel = stringResource(R.string.category_uncategorized)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product?.name ?: stringResource(R.string.screen_product_detail)) },
                actions = {
                    IconButton(onClick = { onEdit(productId) }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                    }
                }
            )
        }
    ) { innerPadding ->
        val current = product
        if (current == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.product_not_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val categoryName = categories.find { it.id == current.categoryId }?.name ?: uncategorizedLabel
            val companyNameById = companies.associate { it.id to it.name }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = current.sku, style = MaterialTheme.typography.bodyMedium, color = PaperMuted)
                        StockStatusChip(status = current.status)
                    }
                }
                item { DetailField(labelRes = R.string.field_category, value = categoryName) }
                item { DetailField(labelRes = R.string.field_reorder_point, value = current.reorderPoint.toString()) }
                item { DetailField(labelRes = R.string.field_quantity, value = current.quantity.toString()) }

                item {
                    Text(
                        text = stringResource(R.string.stock_movements_title),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (movements.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.movements_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = PaperMuted
                        )
                    }
                } else {
                    items(movements, key = { it.id }) { movement ->
                        MovementRow(
                            movement = movement,
                            companyName = companyNameById[movement.partyId]
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            text = { Text(stringResource(R.string.confirm_delete_product)) },
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
private fun DetailField(labelRes: Int, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PaperSurface, MaterialTheme.shapes.small)
            .border(1.5.dp, PaperBorder, MaterialTheme.shapes.small)
            .padding(12.dp)
    ) {
        Text(text = stringResource(labelRes), style = MaterialTheme.typography.labelMedium, color = PaperMuted)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun MovementRow(movement: Movement, companyName: String?) {
    val isIn = movement.movementType == MovementType.IN
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PaperSurface, MaterialTheme.shapes.small)
            .border(1.5.dp, PaperBorder, MaterialTheme.shapes.small)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(if (isIn) R.string.movement_type_in else R.string.movement_type_out),
                color = if (isIn) StockGood else StockBad,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = companyName ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = PaperMuted
            )
        }
        Text(
            text = if (isIn) "+${movement.quantity}" else "-${movement.quantity}",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
