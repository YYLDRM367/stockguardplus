package com.stockguardplus.app.data.repository

import com.stockguardplus.app.data.model.Organization
import kotlinx.coroutines.flow.Flow

interface OrganizationRepository {
    fun observeOrganization(): Flow<Organization?>
}
