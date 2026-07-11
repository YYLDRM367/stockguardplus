package com.stockguardplus.app.ui.screens.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockguardplus.app.R
import com.stockguardplus.app.ui.theme.PaperMuted
import com.stockguardplus.app.ui.theme.StockBad

@Composable
fun AddEditProductScreen(
    productId: String?,
    initialBarcode: String? = null,
    scannedBarcode: String? = null,
    onScanBarcode: () -> Unit = {},
    onSaved: () -> Unit,
    viewModel: AddEditProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(productId) { viewModel.load(productId) }

    LaunchedEffect(Unit) {
        if (!initialBarcode.isNullOrBlank()) viewModel.onBarcodeChange(initialBarcode)
    }
    LaunchedEffect(scannedBarcode) {
        if (!scannedBarcode.isNullOrBlank()) viewModel.onBarcodeChange(scannedBarcode)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    val uncategorizedLabel = stringResource(R.string.category_uncategorized)
    val selectedCategoryName = categories.find { it.id == uiState.categoryId }?.name ?: uncategorizedLabel

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.isEditing) R.string.screen_edit_product else R.string.screen_add_product
                        )
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.field_product_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.sku,
                onValueChange = viewModel::onSkuChange,
                label = { Text(stringResource(R.string.field_sku)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.barcode,
                onValueChange = viewModel::onBarcodeChange,
                label = { Text(stringResource(R.string.field_barcode)) },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = onScanBarcode) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = stringResource(R.string.action_scan_barcode))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (uiState.isEditing) {
                Text(
                    text = stringResource(R.string.quantity_edit_hint, uiState.quantity),
                    style = MaterialTheme.typography.bodySmall,
                    color = PaperMuted
                )
            } else {
                OutlinedTextField(
                    value = uiState.quantity,
                    onValueChange = viewModel::onQuantityChange,
                    label = { Text(stringResource(R.string.field_quantity)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            OutlinedTextField(
                value = uiState.reorderPoint,
                onValueChange = viewModel::onReorderPointChange,
                label = { Text(stringResource(R.string.field_reorder_point)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = categoryMenuExpanded,
                onExpandedChange = { categoryMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedCategoryName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.field_category)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(uncategorizedLabel) },
                        onClick = {
                            viewModel.onCategorySelected("")
                            categoryMenuExpanded = false
                        }
                    )
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                viewModel.onCategorySelected(category.id)
                                categoryMenuExpanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_add_category)) },
                        onClick = {
                            categoryMenuExpanded = false
                            showAddCategoryDialog = true
                        }
                    )
                }
            }

            uiState.errorMessage?.let { message ->
                Text(text = message, color = StockBad, style = MaterialTheme.typography.bodyMedium)
            }

            Button(onClick = viewModel::save, enabled = !uiState.isSaving, modifier = Modifier.fillMaxWidth()) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                } else {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name ->
                viewModel.addAndSelectCategory(name)
                showAddCategoryDialog = false
            }
        )
    }
}

@Composable
private fun AddCategoryDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_add_category)) },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
