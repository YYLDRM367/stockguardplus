package com.stockguardplus.app.ui.screens.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.stockguardplus.app.R
import com.stockguardplus.app.data.model.Company
import com.stockguardplus.app.data.model.MovementType
import com.stockguardplus.app.ui.components.AddCompanyDialog
import com.stockguardplus.app.ui.theme.StockBad
import kotlinx.coroutines.launch

@Composable
fun StockMovementDialog(
    type: MovementType,
    companies: List<Company>,
    errorMessage: String?,
    inProgress: Boolean,
    onDismiss: () -> Unit,
    onAddCompany: suspend (name: String, address: String, phone1: String, phone2: String, email: String) -> String,
    onConfirm: (quantity: Int, partyId: String) -> Unit
) {
    var quantityText by remember { mutableStateOf("") }
    var selectedCompanyId by remember { mutableStateOf("") }
    var companyMenuExpanded by remember { mutableStateOf(false) }
    var showAddCompanyDialog by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val selectedCompanyName = companies.find { it.id == selectedCompanyId }?.name
        ?: stringResource(R.string.company_none)
    val enterQuantityError = stringResource(R.string.error_enter_quantity)
    val selectCompanyError = stringResource(R.string.error_select_company)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (type == MovementType.IN) R.string.action_stock_in else R.string.action_stock_out
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.field_quantity)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                                    selectedCompanyId = company.id
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

                (errorMessage ?: localError)?.let {
                    Text(text = it, color = StockBad, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !inProgress,
                onClick = {
                    val quantity = quantityText.toIntOrNull()
                    when {
                        quantity == null || quantity <= 0 -> localError = enterQuantityError
                        selectedCompanyId.isBlank() -> localError = selectCompanyError
                        else -> {
                            localError = null
                            onConfirm(quantity, selectedCompanyId)
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )

    if (showAddCompanyDialog) {
        AddCompanyDialog(
            onDismiss = { showAddCompanyDialog = false },
            onConfirm = { name, address, phone1, phone2, email ->
                showAddCompanyDialog = false
                scope.launch {
                    selectedCompanyId = onAddCompany(name, address, phone1, phone2, email)
                }
            }
        )
    }
}
