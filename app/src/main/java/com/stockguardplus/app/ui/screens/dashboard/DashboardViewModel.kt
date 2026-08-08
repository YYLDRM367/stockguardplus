package com.stockguardplus.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockguardplus.app.data.model.Company
import com.stockguardplus.app.data.model.Movement
import com.stockguardplus.app.data.model.Product
import com.stockguardplus.app.data.repository.CompanyRepository
import com.stockguardplus.app.data.repository.DemoDataRepository
import com.stockguardplus.app.data.repository.MovementRepository
import com.stockguardplus.app.data.repository.OrganizationRepository
import com.stockguardplus.app.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    productRepository: ProductRepository,
    movementRepository: MovementRepository,
    companyRepository: CompanyRepository,
    private val organizationRepository: OrganizationRepository,
    private val demoDataRepository: DemoDataRepository
) : ViewModel() {

    val products: StateFlow<List<Product>> = productRepository.observeProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentMovements: StateFlow<List<Movement>> = movementRepository.observeRecentMovements(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val companies: StateFlow<List<Company>> = companyRepository.observeCompanies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Only true once the org doc has actually loaded (not null), it hasn't
    // been offered before, and there's no data yet — avoids flashing the
    // dialog for a returning user before their product list arrives.
    val showDemoPrompt: StateFlow<Boolean> = combine(
        organizationRepository.observeOrganization(),
        products
    ) { organization, productList ->
        organization != null && !organization.demoDataOffered && productList.isEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val hasDemoData: StateFlow<Boolean> = products
        .map { list -> list.any { it.isDemo } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun loadDemoData() {
        viewModelScope.launch {
            demoDataRepository.seedDemoData()
        }
    }

    fun skipDemoData() {
        viewModelScope.launch {
            organizationRepository.markDemoDataOffered()
        }
    }

    fun clearDemoData() {
        viewModelScope.launch {
            demoDataRepository.clearDemoData()
        }
    }
}
