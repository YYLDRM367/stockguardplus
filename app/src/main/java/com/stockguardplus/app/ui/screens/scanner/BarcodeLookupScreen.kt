package com.stockguardplus.app.ui.screens.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockguardplus.app.R

@Composable
fun BarcodeLookupScreen(
    barcode: String,
    onFound: (productId: String) -> Unit,
    onAddProductWithBarcode: (barcode: String) -> Unit,
    onCancel: () -> Unit,
    viewModel: BarcodeLookupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(barcode) { viewModel.lookup(barcode) }

    LaunchedEffect(state) {
        val current = state
        if (current is BarcodeLookupState.Found) onFound(current.productId)
    }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        when (state) {
            is BarcodeLookupState.Loading, is BarcodeLookupState.Found -> CircularProgressIndicator()
            is BarcodeLookupState.NotFound -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.product_not_found_by_barcode),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Button(onClick = { onAddProductWithBarcode(barcode) }) {
                    Text(stringResource(R.string.action_add_product_with_barcode))
                }
                OutlinedButton(onClick = onCancel) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    }
}
