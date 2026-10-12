package com.example.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.BackupCategoryDto
import com.example.data.model.BackupPayload
import com.example.data.model.BackupTransactionDto
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date

object DataBackupManager {

    fun serializeBackup(payload: BackupPayload): String {
        val root = JSONObject()
        root.put("version", payload.version)
        root.put("exportedAtMillis", payload.exportedAtMillis)

        val categoriesArray = JSONArray()
        for (cat in payload.categories) {
            val cObj = JSONObject()
            cObj.put("id", cat.id)
            cObj.put("name", cat.name)
            cObj.put("type", cat.type)
            cObj.put("iconKey", cat.iconKey)
            cObj.put("colorHex", cat.colorHex)
            cObj.put("isDefault", cat.isDefault)
            cObj.put("orderIndex", cat.orderIndex)
            categoriesArray.put(cObj)
        }
        root.put("categories", categoriesArray)

        val transactionsArray = JSONArray()
        for (t in payload.transactions) {
            val tObj = JSONObject()
            tObj.put("id", t.id)
            tObj.put("amount", t.amount)
            tObj.put("type", t.type)
            tObj.put("categoryId", t.categoryId)
            tObj.put("wallet", t.wallet)
            tObj.put("dateMillis", t.dateMillis)
            tObj.put("note", t.note)
            tObj.put("createdAtMillis", t.createdAtMillis)
            transactionsArray.put(tObj)
        }
        root.put("transactions", transactionsArray)

        return root.toString(2)
    }

    fun parseBackup(jsonString: String): BackupPayload? {
        return try {
            val root = JSONObject(jsonString)
            val version = root.optInt("version", 1)
            val exportedAtMillis = root.optLong("exportedAtMillis", System.currentTimeMillis())

            val categories = mutableListOf<BackupCategoryDto>()
            val catArray = root.optJSONArray("categories") ?: JSONArray()
            for (i in 0 until catArray.length()) {
                val c = catArray.getJSONObject(i)
                categories.add(
                    BackupCategoryDto(
                        id = c.optLong("id"),
                        name = c.optString("name", "Kategori"),
                        type = c.optString("type", "EXPENSE"),
                        iconKey = c.optString("iconKey", "category"),
                        colorHex = c.optString("colorHex", "#10B981"),
                        isDefault = c.optBoolean("isDefault", false),
                        orderIndex = c.optInt("orderIndex", 0)
                    )
                )
            }

            val transactions = mutableListOf<BackupTransactionDto>()
            val transArray = root.optJSONArray("transactions") ?: JSONArray()
            for (i in 0 until transArray.length()) {
                val t = transArray.getJSONObject(i)
                transactions.add(
                    BackupTransactionDto(
                        id = t.optLong("id"),
                        amount = t.optDouble("amount", 0.0),
                        type = t.optString("type", "EXPENSE"),
                        categoryId = t.optLong("categoryId"),
                        wallet = t.optString("wallet", "Tunai"),
                        dateMillis = t.optLong("dateMillis", System.currentTimeMillis()),
                        note = t.optString("note", ""),
                        createdAtMillis = t.optLong("createdAtMillis", System.currentTimeMillis())
                    )
                )
            }

            BackupPayload(
                version = version,
                exportedAtMillis = exportedAtMillis,
                categories = categories,
                transactions = transactions
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun writeBackupFile(context: Context, payload: BackupPayload): File? {
        return try {
            val backupDir = File(context.cacheDir, "backups")
            if (!backupDir.exists()) backupDir.mkdirs()

            val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", DateUtils.INDONESIAN_LOCALE).format(Date())
            val file = File(backupDir, "CatatKas_Backup_$timestampStr.json")

            val json = serializeBackup(payload)
            val fos = FileOutputStream(file)
            fos.write(json.toByteArray(Charsets.UTF_8))
            fos.flush()
            fos.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun readBackupFromUri(context: Context, uri: Uri): BackupPayload? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val jsonString = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            parseBackup(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
