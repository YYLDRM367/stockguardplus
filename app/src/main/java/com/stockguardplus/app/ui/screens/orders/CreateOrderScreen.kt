package com.stockguardplus.app.ui.screens.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockguardplus.app.R
import com.stockguardplus.app.data.model.OrderType
import com.stockguardplus.app.data.model.Product
import com.stockguardplus.app.ui.components.AddCompanyDialog
import com.stockguardplus.app.ui.theme.StockBad
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CreateOrderScreen(
    type: OrderType,
    onSaved: () -> Unit,
    viewModel: CreateOrderViewModel = hiltViewModel()
) {
    LaunchedEffect(type) { viewModel.setType(type) }

    val uiState by viewModel.uiState.collectAsState()
    val companies by viewModel.companies.collectAsState()
    val products by viewModel.products.collectAsState()
    var companyMenuExpanded by remember { mutableStateOf(false) }
    var showAddCompanyDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val dateFormatter = remember { DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()) }
    val formattedDate = remember(uiState.dateMillis) { dateFormatter.format(Date(uiState.dateMillis)) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    val companyNoneLabel = stringResource(R.string.company_none)
    val selectedCompanyName = companies.find { it.id == uiState.partyId }?.name ?: companyNoneLabel

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (type == OrderType.PURCHASE) R.string.screen_create_purchase_order
                            else R.string.screen_create_sale_order
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
                value = formattedDate,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.field_date)) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.field_date))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.invoiceNumber,
                onValueChange = viewModel::onInvoiceNumberChange,
                label = { Text(stringResource(R.string.field_invoice_number)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.receiptNumber,
                onValueChange = viewModel::onReceiptNumberChange,
                label = { Text(stringResource(R.string.field_receipt_number)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = companyMenuExpanded,
                onExpandedChange = { companyMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedCompanyName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.field_company)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = companyMenuExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = companyMenuExpanded,
                    onDismissRequest = { companyMenuExpanded = false }
                ) {
                    companies.forEach { company ->
                        DropdownMenuItem(
                            text = { Text(company.name) },
                            onClick = {
                                viewModel.onPartySelected(company.id)
                                companyMenuExpanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_add_company)) },
                        onClick = {
                            companyMenuExpanded = false
                            showAddCompanyDialog = true
                        }
                    )
                }
            }

            Text(text = stringResource(R.string.field_order_lines), style = MaterialTheme.typography.titleSmall)

            uiState.lines.forEach { line ->
                OrderLineRow(
                    productId = line.productId,
                    quantityText = line.quantityText,
                    products = products,
                    canRemove = uiState.lines.size > 1,
                    onProductSelected = { viewModel.onLineProductChange(line.key, it) },
                    onQuantityChange = { viewModel.onLineQuantityChange(line.key, it) },
                    onRemove = { viewModel.removeLine(line.key) }
                )
            }

            OutlinedButton(onClick = viewModel::addLine, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_add_line))
            }

            uiState.errorMessageRes?.let { errorRes ->
                Text(text = stringResource(errorRes), color = StockBad, style = MaterialTheme.typography.bodyMedium)
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

    if (showAddCompanyDialog) {
        AddCompanyDialog(
            onDismiss = { showAddCompanyDialog = false },
            onConfirm = { name, address, phone1, phone2, email ->
                showAddCompanyDialog = false
                scope.launch {
                    val id = viewModel.addCompany(name, address, phone1, phone2, email)
                    viewModel.onPartySelected(id)
                }
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onDateChange(it) }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun OrderLineRow(
    productId: String,
    quantityText: String,
    products: List<Product>,
    canRemove: Boolean,
    onProductSelected: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val selectedName = products.find { it.id == productId }?.name ?: stringResource(R.string.field_product)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExposedDropdownMenuBox(
            expanded = menuExpanded,
            onExpandedChange = { menuExpanded = it },
            modifier = Modifier.weight(2f)
        ) {
            OutlinedTextField(
                value = selectedName,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.field_product)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                products.forEach { product ->
                    DropdownMenuItem(
                        text = { Text(product.name) },
                        onClick = {
                            onProductSelected(product.id)
                            menuExpanded = false
                        }
                    )
                }
            }
        }
        OutlinedTextField(
            value = quantityText,
            onValueChange = onQuantityChange,
            label = { Text(stringResource(R.string.field_quantity)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        if (canRemove) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_remove_line))
            }
        }
    }
}
