package com.stockguardplus.app.ui.screens.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockguardplus.app.data.model.Company
import com.stockguardplus.app.data.model.Order
import com.stockguardplus.app.data.model.OrderType
import com.stockguardplus.app.data.repository.CompanyRepository
import com.stockguardplus.app.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class OrdersViewModel @Inject constructor(
    orderRepository: OrderRepository,
    companyRepository: CompanyRepository
) : ViewModel() {

    val purchaseOrders: StateFlow<List<Order>> = orderRepository.observeOrders(OrderType.PURCHASE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val salesOrders: StateFlow<List<Order>> = orderRepository.observeOrders(OrderType.SALE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val companies: StateFlow<List<Company>> = companyRepository.observeCompanies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
