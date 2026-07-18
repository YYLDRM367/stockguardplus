package com.stockguardplus.app.ui.screens.reports

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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockguardplus.app.R
import com.stockguardplus.app.data.model.Movement
import com.stockguardplus.app.data.model.MovementType
import com.stockguardplus.app.ui.theme.PaperBorder
import com.stockguardplus.app.ui.theme.PaperMuted
import com.stockguardplus.app.ui.theme.PaperSurface
import com.stockguardplus.app.ui.theme.StockBad
import com.stockguardplus.app.ui.theme.StockGood
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(viewModel: ReportsViewModel = hiltViewModel()) {
    val filters by viewModel.filters.collectAsState()
    val movements by viewModel.filteredMovements.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val products by viewModel.products.collectAsState()
    val companies by viewModel.companies.collectAsState()

    val productNameById = products.associate { it.id to it.name }
    val companyNameById = companies.associate { it.id to it.name }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var companyMenuExpanded by remember { mutableStateOf(false) }
    var productMenuExpanded by remember { mutableStateOf(false) }

    val dateFormatter = remember { DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()) }
    val context = LocalContext.current

    val csvDateLabel = stringResource(R.string.field_date)
    val csvProductLabel = stringResource(R.string.field_product)
    val csvTypeLabel = stringResource(R.string.report_csv_header_type)
    val csvQuantityLabel = stringResource(R.string.field_quantity)
    val csvCompanyLabel = stringResource(R.string.field_company)
    val csvTypeInLabel = stringResource(R.string.movement_type_in)
    val csvTypeOutLabel = stringResource(R.string.movement_type_out)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_reports)) },
                actions = {
                    IconButton(onClick = {
                        shareMovementsAsCsv(
                            context = context,
                            movements = movements,
                            productNameById = productNameById,
                            companyNameById = companyNameById,
                            dateFormatter = dateFormatter,
                            labels = CsvColumnLabels(
                                date = csvDateLabel,
                                product = csvProductLabel,
                                type = csvTypeLabel,
                                quantity = csvQuantityLabel,
                                company = csvCompanyLabel,
                                typeIn = csvTypeInLabel,
                                typeOut = csvTypeOutLabel
                            )
                        )
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.action_export))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.setQuickRange(QuickRange.TODAY) },
                        label = { Text(stringResource(R.string.report_quick_today)) }
                    )
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.setQuickRange(QuickRange.THIS_WEEK) },
                        label = { Text(stringResource(R.string.report_quick_week)) }
                    )
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.setQuickRange(QuickRange.THIS_MONTH) },
                        label = { Text(stringResource(R.string.report_quick_month)) }
                    )
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = dateFormatter.format(Date(filters.startMillis)),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.field_start_date)) },
                        trailingIcon = {
                            IconButton(onClick = { showStartPicker = true }) {
                                Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.field_start_date))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dateFormatter.format(Date(filters.endMillis)),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.field_end_date)) },
                        trailingIcon = {
                            IconButton(onClick = { showEndPicker = true }) {
                                Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.field_end_date))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = filters.typeFilter == ReportTypeFilter.ALL,
                        onClick = { viewModel.setTypeFilter(ReportTypeFilter.ALL) },
                        label = { Text(stringResource(R.string.report_type_all)) }
                    )
                    FilterChip(
                        selected = filters.typeFilter == ReportTypeFilter.IN,
                        onClick = { viewModel.setTypeFilter(ReportTypeFilter.IN) },
                        label = { Text(stringResource(R.string.movement_type_in)) }
                    )
                    FilterChip(
                        selected = filters.typeFilter == ReportTypeFilter.OUT,
                        onClick = { viewModel.setTypeFilter(ReportTypeFilter.OUT) },
                        label = { Text(stringResource(R.string.movement_type_out)) }
                    )
                }
            }

            item {
                val allCompaniesLabel = stringResource(R.string.field_all_companies)
                val selectedCompanyName = companyNameById[filters.partyId] ?: allCompaniesLabel
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
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = companyMenuExpanded, onDismissRequest = { companyMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(allCompaniesLabel) },
                            onClick = {
                                viewModel.setParty("")
                                companyMenuExpanded = false
                            }
                        )
                        companies.forEach { company ->
                            DropdownMenuItem(
                                text = { Text(company.name) },
                                onClick = {
                                    viewModel.setParty(company.id)
                                    companyMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                val allProductsLabel = stringResource(R.string.field_all_products)
                val selectedProductName = productNameById[filters.productId] ?: allProductsLabel
                ExposedDropdownMenuBox(
                    expanded = productMenuExpanded,
                    onExpandedChange = { productMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedProductName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.field_product)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = productMenuExpanded, onDismissRequest = { productMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(allProductsLabel) },
                            onClick = {
                                viewModel.setProduct("")
                                productMenuExpanded = false
                            }
                        )
                        products.forEach { product ->
                            DropdownMenuItem(
                                text = { Text(product.name) },
                                onClick = {
                                    viewModel.setProduct(product.id)
                                    productMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item { SummaryCard(summary = summary) }

            if (movements.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.report_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(movements, key = { it.id }) { movement ->
                    ReportMovementRow(
                        movement = movement,
                        productName = productNameById[movement.productId].orEmpty(),
                        companyName = companyNameById[movement.partyId].orEmpty(),
                        dateFormatter = dateFormatter
                    )
                }
            }
        }
    }

    if (showStartPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = filters.startMillis)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { viewModel.setDateRange(it, filters.endMillis) }
                    showStartPicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) { DatePicker(state = state) }
    }

    if (showEndPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = filters.endMillis)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { viewModel.setDateRange(filters.startMillis, it) }
                    showEndPicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun SummaryCard(summary: ReportSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PaperSurface, MaterialTheme.shapes.small)
            .border(1.dp, PaperBorder, MaterialTheme.shapes.small)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SummaryItem(value = summary.totalIn.toString(), label = stringResource(R.string.report_total_in), color = StockGood)
        SummaryItem(value = summary.totalOut.toString(), label = stringResource(R.string.report_total_out), color = StockBad)
        SummaryItem(value = summary.movementCount.toString(), label = stringResource(R.string.report_movement_count))
    }
}

@Composable
private fun SummaryItem(value: String, label: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Column {
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = PaperMuted)
    }
}

@Composable
private fun ReportMovementRow(
    movement: Movement,
    productName: String,
    companyName: String,
    dateFormatter: DateFormat
) {
    val isIn = movement.movementType == MovementType.IN
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
