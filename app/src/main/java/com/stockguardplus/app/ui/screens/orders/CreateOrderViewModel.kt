package com.stockguardplus.app.ui.screens.orders

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockguardplus.app.R
import com.stockguardplus.app.data.model.Company
import com.stockguardplus.app.data.model.OrderLine
import com.stockguardplus.app.data.model.OrderType
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

data class OrderLineInput(
    val key: Long,
    val productId: String = "",
    val quantityText: String = ""
)

data class CreateOrderUiState(
    val orderNumber: String = "",
    val partyId: String = "",
    val lines: List<OrderLineInput> = listOf(OrderLineInput(key = 0)),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    @StringRes val errorMessageRes: Int? = null
)

@HiltViewModel
class CreateOrderViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val companyRepository: CompanyRepository,
    productRepository: ProductRepository
) : ViewModel() {

    private var nextLineKey = 1L
    private var type: OrderType = OrderType.PURCHASE

    private val _uiState = MutableStateFlow(CreateOrderUiState())
    val uiState: StateFlow<CreateOrderUiState> = _uiState.asStateFlow()

    val companies: StateFlow<List<Company>> = companyRepository.observeCompanies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<Product>> = productRepository.observeProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setType(type: OrderType) {
        this.type = type
    }

    fun onOrderNumberChange(value: String) {
        _uiState.value = _uiState.value.copy(orderNumber = value)
    }

    fun onPartySelected(partyId: String) {
        _uiState.value = _uiState.value.copy(partyId = partyId)
    }

    fun addLine() {
        val state = _uiState.value
        _uiState.value = state.copy(lines = state.lines + OrderLineInput(key = nextLineKey++))
    }

    fun removeLine(key: Long) {
        val state = _uiState.value
        if (state.lines.size <= 1) return
        _uiState.value = state.copy(lines = state.lines.filterNot { it.key == key })
    }

    fun onLineProductChange(key: Long, productId: String) {
        val state = _uiState.value
        _uiState.value = state.copy(
            lines = state.lines.map { if (it.key == key) it.copy(productId = productId) else it }
        )
    }

    fun onLineQuantityChange(key: Long, value: String) {
        val state = _uiState.value
        val filtered = value.filter { it.isDigit() }
        _uiState.value = state.copy(
            lines = state.lines.map { if (it.key == key) it.copy(quantityText = filtered) else it }
        )
    }

    suspend fun addCompany(name: String, address: String, phone1: String, phone2: String, email: String): String {
        return companyRepository.addCompany(
            Company(
                name = name.trim(),
                address = address.trim(),
                phone1 = phone1.trim(),
                phone2 = phone2.trim(),
                email = email.trim()
            )
        )
    }

    fun save() {
        val state = _uiState.value

        if (state.orderNumber.isBlank()) {
            _uiState.value = state.copy(errorMessageRes = R.string.error_order_number_required)
            return
        }
        if (state.partyId.isBlank()) {
            _uiState.value = state.copy(errorMessageRes = R.string.error_select_company)
            return
        }
        val lines = state.lines.mapNotNull { line ->
            val quantity = line.quantityText.toIntOrNull() ?: return@mapNotNull null
            if (line.productId.isBlank() || quantity <= 0) return@mapNotNull null
            OrderLine(productId = line.productId, quantity = quantity)
        }
        if (lines.isEmpty()) {
            _uiState.value = state.copy(errorMessageRes = R.string.error_at_least_one_line)
            return
        }

        _uiState.value = state.copy(isSaving = true, errorMessageRes = null)
        viewModelScope.launch {
            try {
                orderRepository.createOrder(type, state.orderNumber.trim(), state.partyId, lines)
                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, errorMessageRes = R.string.error_order_save_failed)
            }
        }
    }
}
