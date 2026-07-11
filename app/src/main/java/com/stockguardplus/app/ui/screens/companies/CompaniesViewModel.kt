package com.stockguardplus.app.ui.screens.companies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockguardplus.app.data.model.Company
import com.stockguardplus.app.data.repository.CompanyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompaniesViewModel @Inject constructor(
    private val companyRepository: CompanyRepository
) : ViewModel() {

    val companies: StateFlow<List<Company>> = companyRepository.observeCompanies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCompany(name: String, address: String, phone1: String, phone2: String, email: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        viewModelScope.launch {
            companyRepository.addCompany(
                Company(
                    name = trimmedName,
                    address = address.trim(),
                    phone1 = phone1.trim(),
                    phone2 = phone2.trim(),
                    email = email.trim()
                )
            )
        }
    }

    fun deleteCompany(companyId: String) {
        viewModelScope.launch { companyRepository.deleteCompany(companyId) }
    }
}
