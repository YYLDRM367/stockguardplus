package com.stockguardplus.app.di

import com.stockguardplus.app.data.repository.AuthRepository
import com.stockguardplus.app.data.repository.BillingRepository
import com.stockguardplus.app.data.repository.CategoryRepository
import com.stockguardplus.app.data.repository.CompanyRepository
import com.stockguardplus.app.data.repository.DemoDataRepository
import com.stockguardplus.app.data.repository.FirebaseAuthRepository
import com.stockguardplus.app.data.repository.FirebaseDemoDataRepository
import com.stockguardplus.app.data.repository.FirebaseCategoryRepository
import com.stockguardplus.app.data.repository.FirebaseCompanyRepository
import com.stockguardplus.app.data.repository.FirebaseMovementRepository
import com.stockguardplus.app.data.repository.FirebaseOrderRepository
import com.stockguardplus.app.data.repository.FirebaseOrganizationRepository
import com.stockguardplus.app.data.repository.FirebaseProductRepository
import com.stockguardplus.app.data.repository.FirebaseSubscriptionRepository
import com.stockguardplus.app.data.repository.MovementRepository
import com.stockguardplus.app.data.repository.OrderRepository
import com.stockguardplus.app.data.repository.OrganizationRepository
import com.stockguardplus.app.data.repository.PlayBillingRepository
import com.stockguardplus.app.data.repository.ProductRepository
import com.stockguardplus.app.data.repository.SubscriptionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: FirebaseProductRepository): ProductRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: FirebaseCategoryRepository): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindCompanyRepository(impl: FirebaseCompanyRepository): CompanyRepository

    @Binds
    @Singleton
    abstract fun bindMovementRepository(impl: FirebaseMovementRepository): MovementRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(impl: FirebaseOrderRepository): OrderRepository

    @Binds
    @Singleton
    abstract fun bindOrganizationRepository(impl: FirebaseOrganizationRepository): OrganizationRepository

    @Binds
    @Singleton
    abstract fun bindBillingRepository(impl: PlayBillingRepository): BillingRepository

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(impl: FirebaseSubscriptionRepository): SubscriptionRepository

    @Binds
    @Singleton
    abstract fun bindDemoDataRepository(impl: FirebaseDemoDataRepository): DemoDataRepository
}
