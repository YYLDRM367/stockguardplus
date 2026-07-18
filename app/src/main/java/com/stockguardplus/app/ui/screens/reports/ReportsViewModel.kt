package com.stockguardplus.app.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockguardplus.app.data.model.Company
import com.stockguardplus.app.data.model.Movement
import com.stockguardplus.app.data.model.MovementType
import com.stockguardplus.app.data.model.Product
import com.stockguardplus.app.data.repository.CompanyRepository
import com.stockguardplus.app.data.repository.MovementRepository
import com.stockguardplus.app.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class ReportTypeFilter { ALL, IN, OUT }

enum class QuickRange { TODAY, THIS_WEEK, THIS_MONTH }

data class ReportFilters(
    val startMillis: Long,
    val endMillis: Long,
    val typeFilter: ReportTypeFilter = ReportTypeFilter.ALL,
    val partyId: String = "",
    val productId: String = ""
)

data class ReportSummary(
    val totalIn: Int = 0,
    val totalOut: Int = 0,
    val movementCount: Int = 0
)

private fun startOfToday(): Long =
    Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun endOfToday(): Long =
    Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
    }.timeInMillis

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val movementRepository: MovementRepository,
    productRepository: ProductRepository,
    companyRepository: CompanyRepository
) : ViewModel() {

    private var movementsJob: Job? = null

    private val _filters = MutableStateFlow(ReportFilters(startMillis = startOfToday(), endMillis = endOfToday()))
    val filters: StateFlow<ReportFilters> = _filters.asStateFlow()

    private val _movementsInRange = MutableStateFlow<List<Movement>>(emptyList())

    val products: StateFlow<List<Product>> = productRepository.observeProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val companies: StateFlow<List<Company>> = companyRepository.observeCompanies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredMovements: StateFlow<List<Movement>> = combine(_movementsInRange, _filters) { movements, f ->
        movements.filter { m ->
            val typeMatches = when (f.typeFilter) {
                ReportTypeFilter.ALL -> true
                ReportTypeFilter.IN -> m.movementType == MovementType.IN
                ReportTypeFilter.OUT -> m.movementType == MovementType.OUT
            }
            typeMatches && (f.partyId.isBlank() || m.partyId == f.partyId) &&
                (f.productId.isBlank() || m.productId == f.productId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summary: StateFlow<ReportSummary> = filteredMovements
        .map { movements ->
            ReportSummary(
                totalIn = movements.filter { it.movementType == MovementType.IN }.sumOf { it.quantity },
                totalOut = movements.filter { it.movementType == MovementType.OUT }.sumOf { it.quantity },
                movementCount = movements.size
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportSummary())

    init {
        reloadMovements()
    }

    private fun reloadMovements() {
        movementsJob?.cancel()
        val f = _filters.value
        movementsJob = viewModelScope.launch {
            movementRepository.observeMovementsInRange(f.startMillis, f.endMillis).collect {
                _movementsInRange.value = it
            }
        }
    }

    fun setDateRange(startMillis: Long, endMillis: Long) {
        _filters.value = _filters.value.copy(startMillis = startMillis, endMillis = endMillis)
        reloadMovements()
    }

    fun setQuickRange(range: QuickRange) {
        val calendar = Calendar.getInstance()
        val start: Long
        when (range) {
            QuickRange.TODAY -> {
                start = startOfToday()
            }
            QuickRange.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                start = calendar.timeInMillis
            }
            QuickRange.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                start = calendar.timeInMillis
            }
        }
        setDateRange(start, endOfToday())
    }

    fun setTypeFilter(type: ReportTypeFilter) {
        _filters.value = _filters.value.copy(typeFilter = type)
    }

    fun setParty(partyId: String) {
        _filters.value = _filters.value.copy(partyId = partyId)
    }

    fun setProduct(productId: String) {
        _filters.value = _filters.value.copy(productId = productId)
    }
}
