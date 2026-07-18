package com.stockguardplus.app.ui.screens.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockguardplus.app.data.model.Product
import com.stockguardplus.app.data.model.StockStatus
import com.stockguardplus.app.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class AlertFilter {
    ALL, LOW_STOCK, OUT_OF_STOCK
}

@HiltViewModel
class AlertsViewModel @Inject constructor(
    productRepository: ProductRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(AlertFilter.ALL)
    val filter: StateFlow<AlertFilter> = _filter

    private val allAlertProducts: StateFlow<List<Product>> = productRepository.observeProducts()
        .map { products ->
            products
                .filter { it.status == StockStatus.OUT_OF_STOCK || it.status == StockStatus.LOW_STOCK }
                .sortedBy { if (it.status == StockStatus.OUT_OF_STOCK) 0 else 1 }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alertProducts: StateFlow<List<Product>> = combine(allAlertProducts, _filter) { products, filter ->
        when (filter) {
            AlertFilter.ALL -> products
            AlertFilter.LOW_STOCK -> products.filter { it.status == StockStatus.LOW_STOCK }
            AlertFilter.OUT_OF_STOCK -> products.filter { it.status == StockStatus.OUT_OF_STOCK }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: AlertFilter) {
        _filter.value = filter
    }
}
