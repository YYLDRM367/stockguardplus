package com.stockguardplus.app.ui.screens.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockguardplus.app.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BarcodeLookupState {
    data object Loading : BarcodeLookupState
    data class Found(val productId: String) : BarcodeLookupState
    data object NotFound : BarcodeLookupState
}

@HiltViewModel
class BarcodeLookupViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _state = MutableStateFlow<BarcodeLookupState>(BarcodeLookupState.Loading)
    val state: StateFlow<BarcodeLookupState> = _state.asStateFlow()

    fun lookup(barcode: String) {
        _state.value = BarcodeLookupState.Loading
        viewModelScope.launch {
            val product = productRepository.findProductByBarcode(barcode)
            _state.value = if (product != null) {
                BarcodeLookupState.Found(product.id)
            } else {
                BarcodeLookupState.NotFound
            }
        }
    }
}
