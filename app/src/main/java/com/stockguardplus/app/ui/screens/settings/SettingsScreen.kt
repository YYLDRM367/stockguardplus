package com.stockguardplus.app.ui.screens.settings

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stockguardplus.app.R
import com.stockguardplus.app.ui.theme.PaperMuted
import com.stockguardplus.app.ui.theme.StockBad

private val languageOptions = listOf(
    null to R.string.language_system,
    "tr" to R.string.language_turkish,
    "en" to R.string.language_english
)

private val themeOptions = listOf(
    null to R.string.language_system,
    "light" to R.string.theme_light,
    "dark" to R.string.theme_dark
)

@Composable
fun SettingsScreen(
    onManageCategories: () -> Unit,
    onManageCompanies: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val languageTag by viewModel.languageTag.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val deleteAccountState by viewModel.deleteAccountState.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val currentLanguageLabel = stringResource(
        languageOptions.find { it.first == languageTag }?.second ?: R.string.language_system
    )
    val currentThemeLabel = stringResource(
        themeOptions.find { it.first == themeMode }?.second ?: R.string.language_system
    )

    LaunchedEffect(deleteAccountState) {
        if (deleteAccountState is DeleteAccountState.Success) {
            onSignedOut()
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_settings)) }) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SectionLabel(stringResource(R.string.screen_profile))
            ListItem(
                headlineContent = { Text(stringResource(R.string.field_account_email)) },
                supportingContent = { Text(viewModel.currentUserEmail.orEmpty()) }
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.field_language)) },
                supportingContent = { Text(currentLanguageLabel) },
                modifier = Modifier.clickable { showLanguageDialog = true }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.field_theme)) },
                supportingContent = { Text(currentThemeLabel) },
                modifier = Modifier.clickable { showThemeDialog = true }
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

            HorizontalDivider()
            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.action_delete_account), color = StockBad)
                },
                modifier = Modifier.clickable { showDeleteAccountDialog = true }
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

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.field_theme)) },
            text = {
                Column {
                    themeOptions.forEach { (mode, labelRes) ->
                        val selected = mode == themeMode
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(selected = selected) {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
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
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        DeleteAccountDialog(
            state = deleteAccountState,
            onDismiss = {
                showDeleteAccountDialog = false
                viewModel.resetDeleteAccountState()
            },
            onConfirm = { password -> viewModel.deleteAccount(password) }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = PaperMuted,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun DeleteAccountDialog(
    state: DeleteAccountState,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    val inProgress = state is DeleteAccountState.InProgress

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.confirm_delete_account_title)) },
        text = {
            Column {
                Text(stringResource(R.string.confirm_delete_account_message))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.onboarding_password)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
                if (state is DeleteAccountState.Error) {
                    Text(
                        text = stringResource(R.string.error_delete_account_failed),
                        color = StockBad,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !inProgress && password.isNotBlank(),
                onClick = { onConfirm(password) }
            ) {
                Text(stringResource(R.string.action_delete_account), color = StockBad)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
