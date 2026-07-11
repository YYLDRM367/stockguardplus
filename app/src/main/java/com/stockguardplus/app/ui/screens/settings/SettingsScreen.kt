package com.stockguardplus.app.ui.screens.settings

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockguardplus.app.R

private val languageOptions = listOf(
    null to R.string.language_system,
    "tr" to R.string.language_turkish,
    "en" to R.string.language_english
)

@Composable
fun SettingsScreen(
    onManageCategories: () -> Unit,
    onManageCompanies: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val languageTag by viewModel.languageTag.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val currentLanguageLabel = stringResource(
        languageOptions.find { it.first == languageTag }?.second ?: R.string.language_system
    )

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_settings)) }) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.field_language)) },
                supportingContent = { Text(currentLanguageLabel) },
                modifier = Modifier.clickable { showLanguageDialog = true }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.screen_categories)) },
                modifier = Modifier.clickable(onClick = onManageCategories)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.screen_companies)) },
                modifier = Modifier.clickable(onClick = onManageCompanies)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.action_sign_out)) },
                modifier = Modifier.clickable {
                    viewModel.signOut()
                    onSignedOut()
                }
            )
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.field_language)) },
            text = {
                Column {
                    languageOptions.forEach { (tag, labelRes) ->
                        val selected = tag == languageTag
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(selected = selected) {
                                    viewModel.setLanguage(tag)
                                    showLanguageDialog = false
                                    (context as? Activity)?.recreate()
                                }
                        ) {
                            ListItem(
                                headlineContent = { Text(stringResource(labelRes)) },
                                leadingContent = { RadioButton(selected = selected, onClick = null) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
