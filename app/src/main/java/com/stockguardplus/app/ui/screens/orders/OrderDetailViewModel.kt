package com.stockguardplus.app.ui.screens.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockguardplus.app.data.model.Company
import com.stockguardplus.app.data.model.Order
import com.stockguardplus.app.data.model.Product
import com.stockguardplus.app.data.repository.CompanyRepository
import com.stockguardplus.app.data.repository.OrderRepository
import com.stockguardplus.app.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    companyRepository: CompanyRepository,
    productRepository: ProductRepository
) : ViewModel() {

    private var currentOrderId: String? = null

    private val _order = MutableStateFlow<Order?>(null)
    val order: StateFlow<Order?> = _order.asStateFlow()

    private val _isDeleted = MutableStateFlow(false)
    val isDeleted: StateFlow<Boolean> = _isDeleted.asStateFlow()

    private val _approveError = MutableStateFlow<String?>(null)
    val approveError: StateFlow<String?> = _approveError.asStateFlow()

    private val _isApproving = MutableStateFlow(false)
    val isApproving: StateFlow<Boolean> = _isApproving.asStateFlow()

    val companies: StateFlow<List<Company>> = companyRepository.observeCompanies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<Product>> = productRepository.observeProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun load(orderId: String) {
        if (currentOrderId == orderId) return
        currentOrderId = orderId
        viewModelScope.launch {
            orderRepository.observeOrder(orderId).collect { _order.value = it }
        }
    }

    fun approve() {
        val id = currentOrderId ?: return
        _approveError.value = null
        _isApproving.value = true
        viewModelScope.launch {
            try {
                orderRepository.approveOrder(id)
                _isApproving.value = false
            } catch (e: Exception) {
                _isApproving.value = false
                _approveError.value = e.message ?: "Approval failed."
            }
        }
    }

    fun delete() {
        val id = currentOrderId ?: return
        viewModelScope.launch {
            orderRepository.deleteOrder(id)
            _isDeleted.value = true
        }
    }
}
