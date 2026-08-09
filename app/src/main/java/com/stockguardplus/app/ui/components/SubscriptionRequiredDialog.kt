package com.stockguardplus.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.stockguardplus.app.R

@Composable
fun SubscriptionRequiredDialog(onDismiss: () -> Unit, onSubscribe: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.subscription_required_title)) },
        text = { Text(stringResource(R.string.subscription_required_message)) },
        confirmButton = {
            TextButton(onClick = onSubscribe) {
                Text(stringResource(R.string.action_subscribe))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
