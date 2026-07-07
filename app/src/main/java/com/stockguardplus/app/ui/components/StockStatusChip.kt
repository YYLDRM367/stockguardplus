package com.stockguardplus.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stockguardplus.app.R
import com.stockguardplus.app.data.model.StockStatus
import com.stockguardplus.app.ui.theme.StockBad
import com.stockguardplus.app.ui.theme.StockGood
import com.stockguardplus.app.ui.theme.StockWarn

@Composable
fun StockStatusChip(status: StockStatus, modifier: Modifier = Modifier) {
    val (labelRes, color) = when (status) {
        StockStatus.IN_STOCK -> R.string.status_in_stock to StockGood
        StockStatus.LOW_STOCK -> R.string.status_low_stock to StockWarn
        StockStatus.OUT_OF_STOCK -> R.string.status_out_of_stock to StockBad
    }
    Text(
        text = stringResource(labelRes),
        color = color,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
