package com.example.engine

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ParsedMutationItem(
    val dateMillis: Long,
    val rawDescription: String,
    val cleanedMerchantName: String,
    val amount: Double,
    val isDebit: Boolean, // true = pengeluaran, false = pemasukan
    val isAtmWithdrawal: Boolean,
    val isInternalTransfer: Boolean,
    val targetWalletName: String?
)

data class StatementParseResult(
    val success: Boolean,
    val isPasswordProtected: Boolean = false,
    val errorMessage: String? = null,
    val items: List<ParsedMutationItem> = emptyList(),
    val totalDebit: Double = 0.0,
    val totalCredit: Double = 0.0
)

object MandiriStatementParser {

    private val DATE_FORMATS = listOf(
        SimpleDateFormat("dd/MM/yy", Locale("id", "ID")),
        SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")),
        SimpleDateFormat("dd-MM-yyyy", Locale("id", "ID")),
        SimpleDateFormat("yyyy-MM-dd", Locale("id", "ID"))
    )

    fun parseStatementFile(
        context: Context,
        uri: Uri,
        passwordAttempt: String? = null
    ): StatementParseResult {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
                ?: return StatementParseResult(success = false, errorMessage = "Tidak dapat membuka berkas.")

            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val lines = reader.readLines()
            reader.close()

            // Check if file is encrypted (PDF header or encrypted marker)
            val firstLine = lines.firstOrNull() ?: ""
            if (firstLine.startsWith("%PDF")) {
                // PDF stream parsing
                return parsePdfStream(lines, passwordAttempt)
            } else {
                // CSV or Tab-separated / Excel text
                parseCsvLines(lines)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            StatementParseResult(
                success = false,
                errorMessage = "Gagal memproses berkas: ${e.localizedMessage ?: "Format tidak dikenal"}"
            )
        }
    }

    private fun parseCsvLines(lines: List<String>): StatementParseResult {
        val parsedList = mutableListOf<ParsedMutationItem>()
        var totalDebit = 0.0
        var totalCredit = 0.0

        for (line in lines) {
            val cleanLine = line.trim()
            if (cleanLine.isBlank() || isHeaderLine(cleanLine)) continue

            // Determine delimiter: comma or semicolon or tab
            val delimiter = when {
                cleanLine.contains(";") -> ";"
                cleanLine.contains("\t") -> "\t"
                else -> ","
            }

            val tokens = splitCsvLine(cleanLine, delimiter)
            if (tokens.size < 3) continue

            val dateMillis = extractDate(tokens[0]) ?: continue
            val description = tokens.getOrNull(1)?.trim() ?: continue

            // Find debit and credit columns
            var debit = 0.0
            var credit = 0.0

            if (tokens.size >= 4) {
                // Typical format: Tanggal, Keterangan, Debit, Kredit
                debit = cleanAmount(tokens[2])
                credit = cleanAmount(tokens[3])
            } else if (tokens.size == 3) {
                // Tanggal, Keterangan, Nominal (with sign or type indicator)
                val rawAmount = tokens[2]
                if (rawAmount.contains("CR", ignoreCase = true) || !rawAmount.contains("-")) {
                    credit = cleanAmount(rawAmount)
                } else {
                    debit = cleanAmount(rawAmount)
                }
            }

            val isDebit = debit > 0 || (credit == 0.0 && debit == 0.0)
            val finalAmount = if (isDebit) (if (debit > 0) debit else cleanAmount(tokens.getOrNull(2) ?: "0")) else credit

            if (finalAmount <= 0.0) continue

            if (isDebit) totalDebit += finalAmount else totalCredit += finalAmount

            val isAtm = isAtmWithdrawalText(description)
            val (isTransfer, targetWallet) = detectInternalTransfer(description)
            val cleanedMerchant = cleanMerchantNarration(description)

            parsedList.add(
                ParsedMutationItem(
                    dateMillis = dateMillis,
                    rawDescription = description,
                    cleanedMerchantName = cleanedMerchant,
                    amount = finalAmount,
                    isDebit = isDebit,
                    isAtmWithdrawal = isAtm,
                    isInternalTransfer = isTransfer,
                    targetWalletName = targetWallet
                )
            )
        }

        return if (parsedList.isNotEmpty()) {
            StatementParseResult(
                success = true,
                items = parsedList,
                totalDebit = totalDebit,
                totalCredit = totalCredit
            )
        } else {
            StatementParseResult(
                success = false,
                errorMessage = "Tidak ditemukan baris transaksi yang valid di berkas ini."
            )
        }
    }

    private fun parsePdfStream(lines: List<String>, password: String?): StatementParseResult {
        // Check if PDF requires password
        val combinedText = lines.take(50).joinToString(" ")
        val isEncrypted = combinedText.contains("/Encrypt", ignoreCase = true)

        if (isEncrypted && password.isNullOrBlank()) {
            return StatementParseResult(
                success = false,
                isPasswordProtected = true,
                errorMessage = "Berkas PDF dilindungi kata sandi e-statement bank."
            )
        }

        // Validate 8-digit date format (DDMMYYYY) if provided
        if (isEncrypted && password != null && password.length != 8) {
            return StatementParseResult(
                success = false,
                isPasswordProtected = true,
                errorMessage = "Kata sandi e-statement harus berupa 8 digit tanggal lahir (DDMMYYYY)."
            )
        }

        // Extract text streams from PDF lines
        val extractedLines = mutableListOf<String>()
        val textRegex = Regex("\\((.*?)\\)")

        for (line in lines) {
            val matches = textRegex.findAll(line)
            for (match in matches) {
                val extracted = match.groupValues[1].trim()
                if (extracted.isNotBlank()) extractedLines.add(extracted)
            }
        }

        return if (extractedLines.isNotEmpty()) {
            parseCsvLines(extractedLines)
        } else {
            StatementParseResult(
                success = false,
                errorMessage = "Gagal mengekstrak teks dari PDF. Silakan gunakan ekspor format CSV/Excel dari Livin' untuk akurasi terbaik."
            )
        }
    }

    private fun isHeaderLine(line: String): Boolean {
        val lower = line.lowercase()
        return lower.contains("tanggal") || lower.contains("rekening") ||
                lower.contains("saldo") || lower.contains("keterangan") ||
                lower.contains("statement") || lower.contains("periode")
    }

    private fun extractDate(token: String): Long? {
        val clean = token.replace("\"", "").trim()
        for (format in DATE_FORMATS) {
            try {
                val date = format.parse(clean)
                if (date != null) return date.time
            } catch (_: Exception) {}
        }
        return null
    }

    private fun cleanAmount(token: String): Double {
        val clean = token
            .replace("Rp", "", ignoreCase = true)
            .replace("CR", "", ignoreCase = true)
            .replace("DR", "", ignoreCase = true)
            .replace("IDR", "", ignoreCase = true)
            .replace(".", "")
            .replace(",", ".")
            .replace("\"", "")
            .replace("-", "")
            .trim()
        return clean.toDoubleOrNull() ?: 0.0
    }

    private fun splitCsvLine(line: String, delimiter: String): List<String> {
        val tokens = mutableListOf<String>()
        var inQuotes = false
        val sb = StringBuilder()

        for (char in line) {
            when {
                char == '\"' -> inQuotes = !inQuotes
                char.toString() == delimiter && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                }
                else -> sb.append(char)
            }
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    fun isAtmWithdrawalText(description: String): Boolean {
        val upper = description.uppercase()
        return upper.contains("TARIK TUNAI") || upper.contains("ATM") || upper.contains("TARIKAN")
    }

    fun detectInternalTransfer(description: String): Pair<Boolean, String?> {
        val upper = description.uppercase()
        return when {
            upper.contains("GOPAY") || upper.contains("GO-PAY") || upper.contains("GOTO") ->
                Pair(true, "GoPay Pribadi")
            upper.contains("OVO") ->
                Pair(true, "OVO Pribadi")
            upper.contains("MERCHANT") || upper.contains("GOBIZ") ->
                Pair(true, "GoPay Merchant QRIS")
            else -> Pair(false, null)
        }
    }

    fun cleanMerchantNarration(description: String): String {
        var clean = description
            .replace(Regex("(?i)QRIS\\s*"), "")
            .replace(Regex("(?i)TRF\\s*BI-FAST\\s*"), "")
            .replace(Regex("(?i)TRANSFER\\s*(KE|DARI)?\\s*"), "")
            .replace(Regex("(?i)TOP\\s*UP\\s*"), "")
            .replace(Regex("(?i)PEMBELIAN\\s*"), "")
            .replace(Regex("(?i)PEMBAYARAN\\s*"), "")
            .replace(Regex("\\b\\d{6,}\\b"), "") // Remove long transaction numbers
            .replace(Regex("[#*]"), " ")
            .trim()

        if (clean.length > 30) {
            clean = clean.take(28) + ".."
        }
        return if (clean.isNotBlank()) clean else description.take(25)
    }
}
