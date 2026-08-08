package com.stockguardplus.app.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.stockguardplus.app.R
import com.stockguardplus.app.ui.theme.PaperMuted
import com.stockguardplus.app.ui.theme.StockBad
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onSignedIn: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.isSignedIn) {
        if (uiState.isSignedIn) onSignedIn()
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            if (uiState.isSignUpMode) {
                OutlinedTextField(
                    value = uiState.businessName,
                    onValueChange = viewModel::onBusinessNameChange,
                    label = { Text(stringResource(R.string.onboarding_business_name)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            }

            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text(stringResource(R.string.onboarding_email)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text(stringResource(R.string.onboarding_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            )

            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = StockBad,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Button(onClick = viewModel::submit, enabled = !uiState.isLoading, modifier = Modifier.fillMaxWidth()) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                } else {
                    Text(
                        stringResource(
                            if (uiState.isSignUpMode) R.string.onboarding_sign_up else R.string.onboarding_sign_in
                        )
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.onboarding_or),
                    color = PaperMuted,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            OutlinedButton(
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    coroutineScope.launch {
                        try {
                            val credentialManager = CredentialManager.create(context)
                            val googleIdOption = GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId(context.getString(R.string.default_web_client_id))
                                .build()
                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build()
                            val response = credentialManager.getCredential(context, request)
                            val credential = response.credential
                            if (credential is CustomCredential &&
                                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                            ) {
                                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                viewModel.signInWithGoogle(googleIdTokenCredential.idToken)
                            }
                        } catch (e: GetCredentialException) {
                            // User cancelled or no saved/available Google account — nothing to
                            // report, they can just try again or use email/password instead.
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.onboarding_google_sign_in))
            }

            TextButton(onClick = viewModel::toggleMode) {
                Text(
                    stringResource(
                        if (uiState.isSignUpMode) {
                            R.string.onboarding_toggle_to_sign_in
                        } else {
                            R.string.onboarding_toggle_to_sign_up
                        }
                    )
                )
            }
        }
    }
}
