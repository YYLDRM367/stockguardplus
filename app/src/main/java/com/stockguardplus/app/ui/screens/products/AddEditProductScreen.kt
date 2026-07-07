package com.stockguardplus.app.ui.screens.products

import androidx.compose.runtime.Composable
import com.stockguardplus.app.R
import com.stockguardplus.app.ui.components.PlaceholderScreen

@Composable
fun AddEditProductScreen(productId: String?) {
    val titleRes = if (productId != null) R.string.screen_edit_product else R.string.screen_add_product
    PlaceholderScreen(titleRes = titleRes)
}
