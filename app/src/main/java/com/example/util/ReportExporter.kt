package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithCategory
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date

object ReportExporter {

    fun exportToCsv(
        context: Context,
        periodName: String,
        transactions: List<TransactionWithCategory>
    ): File? {
        return try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val fileName = "Laporan_CatatKas_${periodName.replace(" ", "_")}.csv"
            val file = File(reportsDir, fileName)

            val writer = file.bufferedWriter(Charsets.UTF_8)
            // Add UTF-8 BOM so Excel opens it with correct encoding
            writer.write("\uFEFF")

            // Headers
            writer.write("Tanggal,Waktu,Jenis,Kategori,Nominal (Rp),Dompet/Rekening,Catatan\n")

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", DateUtils.INDONESIAN_LOCALE)
            val timeFormat = SimpleDateFormat("HH:mm", DateUtils.INDONESIAN_LOCALE)

            var totalIncome = 0.0
            var totalExpense = 0.0

            for (item in transactions) {
                val t = item.transaction
                val dateStr = dateFormat.format(Date(t.dateMillis))
                val timeStr = timeFormat.format(Date(t.dateMillis))
                val typeStr = if (t.type == TransactionType.INCOME) "Pemasukan" else "Pengeluaran"
                val categoryStr = escapeCsv(item.category?.name ?: "Tanpa Kategori")
                val amountStr = t.amount.toLong().toString()
                val walletStr = escapeCsv(t.wallet)
                val noteStr = escapeCsv(t.note)

                if (t.type == TransactionType.INCOME) {
                    totalIncome += t.amount
                } else {
                    totalExpense += t.amount
                }

                writer.write("$dateStr,$timeStr,$typeStr,$categoryStr,$amountStr,$walletStr,$noteStr\n")
            }

            // Summary Rows
            writer.write("\n")
            writer.write("RINGKASAN LAPORAN,,,,,,,\n")
            writer.write("Total Pemasukan,,,$totalIncome,,,,\n")
            writer.write("Total Pengeluaran,,,$totalExpense,,,,\n")
            writer.write("Saldo Bersih,,,${totalIncome - totalExpense},,,,\n")

            writer.flush()
            writer.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportToPdf(
        context: Context,
        periodName: String,
        transactions: List<TransactionWithCategory>
    ): File? {
        return try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val fileName = "Laporan_CatatKas_${periodName.replace(" ", "_")}.pdf"
            val file = File(reportsDir, fileName)

            val document = PdfDocument()
            val pageWidth = 595 // A4 standard width in points
            val pageHeight = 842 // A4 standard height in points
            var pageNumber = 1

            var totalIncome = 0.0
            var totalExpense = 0.0
            for (t in transactions) {
                if (t.transaction.type == TransactionType.INCOME) totalIncome += t.transaction.amount
                else totalExpense += t.transaction.amount
            }

            val paint = Paint().apply { isAntiAlias = true }
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas

            var y = 40f

            // Header Banner
            paint.color = Color.rgb(15, 23, 42) // Slate dark
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 90f, paint)

            paint.color = Color.rgb(16, 185, 129) // Emerald accent
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("CatatKas", 36f, 44f, paint)

            paint.color = Color.WHITE
            paint.textSize = 12f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("Laporan Arus Kas & Keuangan Pribadi", 36f, 64f, paint)

            paint.color = Color.rgb(203, 213, 225)
            paint.textSize = 10f
            val exportDate = SimpleDateFormat("dd MMM yyyy, HH:mm", DateUtils.INDONESIAN_LOCALE).format(Date())
            canvas.drawText("Diekspor: $exportDate", (pageWidth - 180).toFloat(), 64f, paint)

            y = 110f

            // Period Subtitle
            paint.color = Color.rgb(30, 41, 59)
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Periode: $periodName", 36f, y, paint)

            y += 20f

            // Summary Boxes
            val boxWidth = (pageWidth - 72f - 20f) / 3f
            val boxHeight = 52f

            // Income Box
            paint.color = Color.rgb(236, 253, 245)
            canvas.drawRoundRect(36f, y, 36f + boxWidth, y + boxHeight, 8f, 8f, paint)
            paint.color = Color.rgb(5, 150, 105)
            paint.textSize = 9f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("TOTAL PEMASUKAN", 44f, y + 18f, paint)
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(CurrencyFormatter.formatRupiah(totalIncome), 44f, y + 38f, paint)

            // Expense Box
            val expX = 36f + boxWidth + 10f
            paint.color = Color.rgb(254, 242, 242)
            canvas.drawRoundRect(expX, y, expX + boxWidth, y + boxHeight, 8f, 8f, paint)
            paint.color = Color.rgb(220, 38, 38)
            paint.textSize = 9f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("TOTAL PENGELUARAN", expX + 8f, y + 18f, paint)
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(CurrencyFormatter.formatRupiah(totalExpense), expX + 8f, y + 38f, paint)

            // Net Balance Box
            val balX = expX + boxWidth + 10f
            val netBalance = totalIncome - totalExpense
            paint.color = if (netBalance >= 0) Color.rgb(240, 249, 255) else Color.rgb(254, 242, 242)
            canvas.drawRoundRect(balX, y, balX + boxWidth, y + boxHeight, 8f, 8f, paint)
            paint.color = if (netBalance >= 0) Color.rgb(2, 132, 199) else Color.rgb(220, 38, 38)
            paint.textSize = 9f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("SALDO BERSIH", balX + 8f, y + 18f, paint)
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(CurrencyFormatter.formatRupiah(netBalance), balX + 8f, y + 38f, paint)

            y += boxHeight + 25f

            // Table Header
            paint.color = Color.rgb(241, 245, 249)
            canvas.drawRect(36f, y, (pageWidth - 36).toFloat(), y + 24f, paint)

            paint.color = Color.rgb(71, 85, 105)
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            canvas.drawText("TANGGAL", 44f, y + 16f, paint)
            canvas.drawText("KATEGORI", 120f, y + 16f, paint)
            canvas.drawText("DOMPET", 240f, y + 16f, paint)
            canvas.drawText("CATATAN", 310f, y + 16f, paint)
            canvas.drawText("NOMINAL (RP)", 450f, y + 16f, paint)

            y += 28f

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", DateUtils.INDONESIAN_LOCALE)

            // Table Rows
            for ((index, item) in transactions.withIndex()) {
                if (y > pageHeight - 60) {
                    // Start new page
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    y = 40f

                    // Draw continuation table header
                    paint.color = Color.rgb(241, 245, 249)
                    canvas.drawRect(36f, y, (pageWidth - 36).toFloat(), y + 20f, paint)
                    paint.color = Color.rgb(71, 85, 105)
                    paint.textSize = 9f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("TANGGAL", 44f, y + 14f, paint)
                    canvas.drawText("KATEGORI", 120f, y + 14f, paint)
                    canvas.drawText("DOMPET", 240f, y + 14f, paint)
                    canvas.drawText("CATATAN", 310f, y + 14f, paint)
                    canvas.drawText("NOMINAL (RP)", 450f, y + 14f, paint)
                    y += 24f
                }

                // Alternate row background
                if (index % 2 == 1) {
                    paint.color = Color.rgb(248, 250, 252)
                    canvas.drawRect(36f, y - 2f, (pageWidth - 36).toFloat(), y + 18f, paint)
                }

                val t = item.transaction
                paint.typeface = Typeface.DEFAULT
                paint.textSize = 9f
                paint.color = Color.rgb(51, 65, 85)

                canvas.drawText(dateFormat.format(Date(t.dateMillis)), 44f, y + 12f, paint)

                val catName = truncateText(item.category?.name ?: "Tanpa Kategori", 18)
                canvas.drawText(catName, 120f, y + 12f, paint)

                val walletName = truncateText(t.wallet, 10)
                canvas.drawText(walletName, 240f, y + 12f, paint)

                val note = truncateText(if (t.note.isNotBlank()) t.note else "-", 22)
                canvas.drawText(note, 310f, y + 12f, paint)

                // Amount
                val isInc = t.type == TransactionType.INCOME
                paint.color = if (isInc) Color.rgb(16, 185, 129) else Color.rgb(239, 68, 68)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val formattedAmount = CurrencyFormatter.formatSignedRupiah(t.amount, isInc)
                canvas.drawText(formattedAmount, 450f, y + 12f, paint)

                y += 20f
            }

            // Footer
            paint.color = Color.rgb(148, 163, 184)
            paint.textSize = 8f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("Dicetak dari CatatKas Android - Laporan Keuangan Pribadi", 36f, (pageHeight - 20).toFloat(), paint)

            document.finishPage(page)

            val outputStream = FileOutputStream(file)
            document.writeTo(outputStream)
            outputStream.close()
            document.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareExportFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun truncateText(text: String, maxChars: Int): String {
        return if (text.length > maxChars) text.take(maxChars - 2) + ".." else text
    }
}
