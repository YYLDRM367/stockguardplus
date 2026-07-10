package com.stockguardplus.app.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stockguardplus.app.data.model.Category
import com.stockguardplus.app.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _newCategoryName = MutableStateFlow("")
    val newCategoryName: StateFlow<String> = _newCategoryName.asStateFlow()

    fun onNewCategoryNameChange(value: String) {
        _newCategoryName.value = value
    }

    fun addCategory() {
        val name = _newCategoryName.value.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            categoryRepository.addCategory(name)
            _newCategoryName.value = ""
        }
    }

    fun renameCategory(categoryId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch { categoryRepository.renameCategory(categoryId, trimmed) }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch { categoryRepository.deleteCategory(categoryId) }
    }
}
