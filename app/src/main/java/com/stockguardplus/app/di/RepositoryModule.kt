package com.stockguardplus.app.di

import com.stockguardplus.app.data.repository.AuthRepository
import com.stockguardplus.app.data.repository.CategoryRepository
import com.stockguardplus.app.data.repository.FirebaseAuthRepository
import com.stockguardplus.app.data.repository.FirebaseCategoryRepository
import com.stockguardplus.app.data.repository.FirebaseProductRepository
import com.stockguardplus.app.data.repository.ProductRepository
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
}
