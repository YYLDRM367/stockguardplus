package com.stockguardplus.app.data.repository

import com.stockguardplus.app.data.model.Company
import kotlinx.coroutines.flow.Flow

interface CompanyRepository {
    fun observeCompanies(): Flow<List<Company>>

    suspend fun addCompany(company: Company): String

    suspend fun deleteCompany(companyId: String)
}
