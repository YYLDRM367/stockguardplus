package com.stockguardplus.app.ui.screens.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockguardplus.app.data.model.Category
import com.stockguardplus.app.data.model.Product
import com.stockguardplus.app.data.repository.CategoryRepository
import com.stockguardplus.app.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddProductUiState(
    val name: String = "",
    val sku: String = "",
    val barcode: String = "",
    val quantity: String = "",
    val reorderPoint: String = "",
    val categoryId: String = "",
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AddEditProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private var editingProductId: String? = null

    private val _uiState = MutableStateFlow(AddProductUiState())
    val uiState: StateFlow<AddProductUiState> = _uiState.asStateFlow()

    val categories: StateFlow<List<Category>> = categoryRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun load(productId: String?) {
        if (productId == null || editingProductId == productId) return
        editingProductId = productId
        _uiState.value = _uiState.value.copy(isEditing = true, isLoading = true)
        viewModelScope.launch {
            val product = productRepository.observeProduct(productId)
            product.collect { loaded ->
                if (loaded != null) {
                    _uiState.value = _uiState.value.copy(
                        name = loaded.name,
                        sku = loaded.sku,
                        barcode = loaded.barcode,
                        quantity = loaded.quantity.toString(),
                        reorderPoint = loaded.reorderPoint.toString(),
                        categoryId = loaded.categoryId,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value)
    }

    fun onSkuChange(value: String) {
        _uiState.value = _uiState.value.copy(sku = value)
    }

    fun onBarcodeChange(value: String) {
        _uiState.value = _uiState.value.copy(barcode = value)
    }

    fun onQuantityChange(value: String) {
        _uiState.value = _uiState.value.copy(quantity = value.filter { it.isDigit() })
    }

    fun onReorderPointChange(value: String) {
        _uiState.value = _uiState.value.copy(reorderPoint = value.filter { it.isDigit() })
    }

    fun onCategorySelected(categoryId: String) {
        _uiState.value = _uiState.value.copy(categoryId = categoryId)
    }

    fun addAndSelectCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val newId = categoryRepository.addCategory(trimmed)
            _uiState.value = _uiState.value.copy(categoryId = newId)
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank() || state.sku.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Name and SKU are required.")
            return
        }

        _uiState.value = state.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val id = editingProductId
                if (id != null) {
                    productRepository.updateProductDetails(
                        productId = id,
                        name = state.name.trim(),
                        sku = state.sku.trim(),
                        barcode = state.barcode.trim(),
                        reorderPoint = state.reorderPoint.toIntOrNull() ?: 0,
                        categoryId = state.categoryId
                    )
                } else {
                    productRepository.addProduct(
                        Product(
                            name = state.name.trim(),
                            sku = state.sku.trim(),
                            barcode = state.barcode.trim(),
                            quantity = state.quantity.toIntOrNull() ?: 0,
                            reorderPoint = state.reorderPoint.toIntOrNull() ?: 0,
                            categoryId = state.categoryId
                        )
                    )
                }
                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = e.message ?: "Could not save the product.")
            }
        }
    }
}
