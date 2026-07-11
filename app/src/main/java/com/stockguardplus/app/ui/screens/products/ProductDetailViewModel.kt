package com.stockguardplus.app.ui.screens.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockguardplus.app.data.model.Category
import com.stockguardplus.app.data.model.Company
import com.stockguardplus.app.data.model.Movement
import com.stockguardplus.app.data.model.Product
import com.stockguardplus.app.data.repository.CategoryRepository
import com.stockguardplus.app.data.repository.CompanyRepository
import com.stockguardplus.app.data.repository.MovementRepository
import com.stockguardplus.app.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    companyRepository: CompanyRepository,
    private val movementRepository: MovementRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    private var currentProductId: String? = null
    private var observeProductJob: Job? = null
    private var observeMovementsJob: Job? = null

    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> = _product.asStateFlow()

    private val _movements = MutableStateFlow<List<Movement>>(emptyList())
    val movements: StateFlow<List<Movement>> = _movements.asStateFlow()

    private val _isDeleted = MutableStateFlow(false)
    val isDeleted: StateFlow<Boolean> = _isDeleted.asStateFlow()

    val categories: StateFlow<List<Category>> = categoryRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val companies: StateFlow<List<Company>> = companyRepository.observeCompanies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun load(productId: String) {
        if (currentProductId == productId) return
        currentProductId = productId

        observeProductJob?.cancel()
        observeProductJob = viewModelScope.launch {
            productRepository.observeProduct(productId).collect { _product.value = it }
        }

        observeMovementsJob?.cancel()
        observeMovementsJob = viewModelScope.launch {
            movementRepository.observeMovements(productId).collect { _movements.value = it }
        }
    }

    fun delete() {
        val id = currentProductId ?: return
        viewModelScope.launch {
            productRepository.deleteProduct(id)
            _isDeleted.value = true
        }
    }
}
