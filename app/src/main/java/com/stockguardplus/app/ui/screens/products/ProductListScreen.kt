package com.stockguardplus.app.ui.screens.products

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockguardplus.app.R
import com.stockguardplus.app.data.model.Category
import com.stockguardplus.app.data.model.Product
import com.stockguardplus.app.ui.components.StockStatusChip
import com.stockguardplus.app.ui.theme.PaperBorder
import com.stockguardplus.app.ui.theme.PaperMuted
import com.stockguardplus.app.ui.theme.PaperSurface

@Composable
fun ProductListScreen(
    onProductClick: (String) -> Unit,
    onAddProduct: () -> Unit,
    onScanBarcode: () -> Unit,
    viewModel: ProductListViewModel = hiltViewModel()
) {
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchText by viewModel.searchText.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val hasAnyProducts = products.isNotEmpty() || searchText.isNotBlank() || selectedCategoryId != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_products)) },
                actions = {
                    IconButton(onClick = onScanBarcode) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = stringResource(R.string.action_scan_barcode))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProduct) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add_product))
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = viewModel::onSearchTextChange,
                    placeholder = { Text(stringResource(R.string.field_search_product)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                CategoryFilterRow(
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    onCategorySelected = viewModel::onCategorySelected
                )
            }

            if (products.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(
                            if (hasAnyProducts) R.string.products_empty_filtered else R.string.products_empty
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductRow(product = product, onClick = { onProductClick(product.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onCategorySelected(null) },
                label = { Text(stringResource(R.string.category_all)) }
            )
        }
        item {
            FilterChip(
                selected = selectedCategoryId == "",
                onClick = { onCategorySelected("") },
                label = { Text(stringResource(R.string.category_uncategorized)) }
            )
        }
        items(categories, key = { it.id }) { category ->
            FilterChip(
                selected = selectedCategoryId == category.id,
                onClick = { onCategorySelected(category.id) },
                label = { Text(category.name) }
            )
        }
    }
}

@Composable
private fun ProductRow(product: Product, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(PaperSurface, MaterialTheme.shapes.small)
            .border(1.5.dp, PaperBorder, MaterialTheme.shapes.small)
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = product.name, style = MaterialTheme.typography.bodyLarge)
            Text(text = product.quantity.toString(), style = MaterialTheme.typography.bodyLarge)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = product.sku, style = MaterialTheme.typography.bodyMedium, color = PaperMuted)
            StockStatusChip(status = product.status)
        }
    }
}
