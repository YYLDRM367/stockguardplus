package com.stockguardplus.app.ui.screens.reports

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.stockguardplus.app.data.model.Movement
import com.stockguardplus.app.data.model.MovementType
import java.io.File
import java.text.DateFormat

data class CsvColumnLabels(
    val date: String,
    val product: String,
    val type: String,
    val quantity: String,
    val company: String,
    val typeIn: String,
    val typeOut: String
)

fun shareMovementsAsCsv(
    context: Context,
    movements: List<Movement>,
    productNameById: Map<String, String>,
    companyNameById: Map<String, String>,
    dateFormatter: DateFormat,
    labels: CsvColumnLabels
) {
    val header = listOf(labels.date, labels.product, labels.type, labels.quantity, labels.company)
    val rows = movements.map { movement ->
        listOf(
            movement.timestamp?.toDate()?.let { dateFormatter.format(it) }.orEmpty(),
            productNameById[movement.productId].orEmpty(),
            if (movement.movementType == MovementType.IN) labels.typeIn else labels.typeOut,
            movement.quantity.toString(),
            companyNameById[movement.partyId].orEmpty()
        )
    }

    val csv = buildString {
        appendLine(header.joinToString(",") { csvEscape(it) })
        rows.forEach { row -> appendLine(row.joinToString(",") { csvEscape(it) }) }
    }

    val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(exportsDir, "stockguard-report-${System.currentTimeMillis()}.csv")
    file.writeText(csv)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, null))
}

private fun csvEscape(value: String): String = "\"${value.replace("\"", "\"\"")}\""
