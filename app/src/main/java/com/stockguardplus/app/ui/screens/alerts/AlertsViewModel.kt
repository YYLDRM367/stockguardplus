package com.stockguardplus.app.ui.screens.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockguardplus.app.data.model.Product
import com.stockguardplus.app.data.model.StockStatus
import com.stockguardplus.app.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    productRepository: ProductRepository
) : ViewModel() {

    val alertProducts: StateFlow<List<Product>> = productRepository.observeProducts()
        .map { products ->
            products
                .filter { it.status == StockStatus.OUT_OF_STOCK || it.status == StockStatus.LOW_STOCK }
                .sortedBy { if (it.status == StockStatus.OUT_OF_STOCK) 0 else 1 }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
