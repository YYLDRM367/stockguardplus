package com.stockguardplus.app.ui.screens.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockguardplus.app.R
import com.stockguardplus.app.data.model.Category

@Composable
fun CategoriesScreen(viewModel: CategoriesViewModel = hiltViewModel()) {
    val categories by viewModel.categories.collectAsState()
    val newCategoryName by viewModel.newCategoryName.collectAsState()
    var renamingCategory by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.screen_categories)) }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = viewModel::onNewCategoryNameChange,
                    label = { Text(stringResource(R.string.field_new_category_name)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = viewModel::addCategory) {
                    Text(stringResource(R.string.action_add))
                }
            }

            LazyColumn(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(categories, key = { it.id }) { category ->
                    ListItem(
                        headlineContent = { Text(category.name) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { renamingCategory = category }) {
                                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_rename))
                                }
                                IconButton(onClick = { viewModel.deleteCategory(category.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    renamingCategory?.let { category ->
        RenameCategoryDialog(
            category = category,
            onDismiss = { renamingCategory = null },
            onConfirm = { newName ->
                viewModel.renameCategory(category.id, newName)
                renamingCategory = null
            }
        )
    }
}

@Composable
private fun RenameCategoryDialog(category: Category, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember(category.id) { mutableStateOf(category.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_rename)) },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
