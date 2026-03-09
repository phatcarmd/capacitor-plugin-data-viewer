package com.phatvuong.plugin

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class DatabaseRepository private constructor() {
    companion object {
        @Volatile
        private var instance: DatabaseRepository? = null

        fun getInstance() = instance ?: synchronized(this) {
            instance ?: DatabaseRepository().also { instance = it }
        }
    }

    fun getSharedPreferencesFiles(context: Context): List<String> {
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        return if (prefsDir.exists() && prefsDir.isDirectory) {
            prefsDir.listFiles { _, name -> name.endsWith(".xml") }
                ?.map { it.name.removeSuffix(".xml") } ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun getDatabaseFiles(context: Context): List<String> {
        val dbDir = context.getDatabasePath("dummy.db").parentFile

        if (dbDir?.exists() != true) {
            return emptyList()
        }

        val files = dbDir.listFiles()

        if (files.isNullOrEmpty()) {
            return emptyList()
        }

        return files.filter {
            !it.name.endsWith("-journal") && !it.name.endsWith("-shm") && !it.name.endsWith("-wal")
        }.map {
            it.name
        }
    }

    fun getTables(context: Context, dbName: String): List<String> {
        val tableList = mutableListOf<String>()

        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) {
            return emptyList()
        }

        SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'android_metadata' AND name NOT LIKE 'sqlite_%'", null).use { cursor ->
                while (cursor.moveToNext()) {
                    tableList.add(cursor.getString(0))
                }
            }
        }
        return tableList
    }

    fun getTableData(context: Context, dbName: String, tableName: String, page: Int, pageSize: Int, filters: List<FilterCondition>) : Pair<List<String>, List<List<String>>> {
        val columns = mutableListOf<String>()
        val rows = mutableListOf<List<String>>()
        val dbFile = context.getDatabasePath(dbName)

        SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("PRAGMA table_info($tableName)", null).use { cursor ->
                while (cursor.moveToNext()) {
                    columns.add(cursor.getString(1))
                }
            }

            val whereClause = buildWhereClause(filters)
            val offset = page * pageSize
            val query = "SELECT * FROM $tableName $whereClause LIMIT $pageSize OFFSET $offset"
            db.rawQuery(query, null).use { cursor ->
                while (cursor.moveToNext()) {
                    val row = mutableListOf<String>()
                    for (i in 0 until cursor.columnCount) {
                        val type = cursor.getType(i)
                        val value = when (type) {
                            Cursor.FIELD_TYPE_STRING -> cursor.getString(i)
                            Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i).toString()
                            Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(i).toString()
                            Cursor.FIELD_TYPE_BLOB -> {
                                val bytes = cursor.getBlob(i)
                                "[BLOB: ${bytes?.size ?: 0} bytes]" // Hiển thị thông tin thay vì nội dung
                            }
                            Cursor.FIELD_TYPE_NULL -> "NULL"
                            else -> "UNKNOWN"
                        }
                        row.add(value ?: "NULL")
                    }
                    rows.add(row)
                }
            }
        }
        return Pair(columns, rows)
    }

    fun getSharedPrefsData(context: Context, fileName: String): List<Pair<String, String>> {
        val prefs = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
        return prefs.all.map { entry ->
            val value = when (val v = entry.value) {
                is Set<*> -> v.joinToString(", ")
                else -> v.toString()
            }
            entry.key to value
        }.sortedBy { it.first }
    }

    fun getSharedPrefsEntries(context: Context, fileName: String): List<SharedPrefEntry> {
        val prefs = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
        val typeMeta = getTypeMetaPrefs(context, fileName)

        return prefs.all.map { entry ->
            val key = entry.key
            val value = entry.value
            val runtimeType = runtimeTypeName(value)
            val metaType = typeMeta.getString(key, null)
            val displayType = when {
                runtimeType != "String" -> runtimeType
                !metaType.isNullOrBlank() -> metaType
                value is String -> inferStringValueType(value)
                else -> runtimeType
            }

            SharedPrefEntry(
                key = key,
                value = valueToDisplay(value),
                type = displayType
            )
        }.sortedBy { it.key }
    }

    fun upsertSharedPrefEntry(
        context: Context,
        fileName: String,
        key: String,
        type: String,
        rawInput: String,
        oldKey: String? = null
    ): String? {
        val trimmedKey = key.trim()
        if (trimmedKey.isEmpty()) return "Key is required."

        val prefs = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
        val meta = getTypeMetaPrefs(context, fileName)

        if (!oldKey.isNullOrEmpty() && oldKey != trimmedKey && prefs.contains(trimmedKey)) {
            return "Key '$trimmedKey' already exists."
        }
        if (oldKey.isNullOrEmpty() && prefs.contains(trimmedKey)) {
            return "Key '$trimmedKey' already exists."
        }

        val parsed = parseTypedInput(type, rawInput) ?: return validationError(type)

        val editor = prefs.edit()
        val metaEditor = meta.edit()

        if (!oldKey.isNullOrEmpty() && oldKey != trimmedKey) {
            editor.remove(oldKey)
            metaEditor.remove(oldKey)
        }

        when (parsed) {
            is String -> editor.putString(trimmedKey, parsed)
            is Int -> editor.putInt(trimmedKey, parsed)
            is Long -> editor.putLong(trimmedKey, parsed)
            is Float -> editor.putFloat(trimmedKey, parsed)
            is Boolean -> editor.putBoolean(trimmedKey, parsed)
            is Set<*> -> {
                @Suppress("UNCHECKED_CAST")
                editor.putStringSet(trimmedKey, parsed as Set<String>)
            }
            else -> return "Unsupported type '$type'."
        }

        // Keep display type for string-backed custom types.
        if (type == "Data" || type == "Dictionary" || type == "Array") {
            metaEditor.putString(trimmedKey, type)
        } else {
            metaEditor.remove(trimmedKey)
        }

        val saveSuccess = editor.commit()
        val metaSuccess = metaEditor.commit()
        return if (saveSuccess && metaSuccess) null else "Failed to save changes."
    }

    fun deleteSharedPrefEntry(context: Context, fileName: String, key: String): Boolean {
        val prefs = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
        val meta = getTypeMetaPrefs(context, fileName)
        val saveSuccess = prefs.edit().remove(key).commit()
        val metaSuccess = meta.edit().remove(key).commit()
        return saveSuccess && metaSuccess
    }

    fun buildWhereClause(filters: List<FilterCondition>): String {
        if (filters.isEmpty()) return ""

        val conditions = filters.filter { it.column.isNotEmpty() && it.value.isNotEmpty() }
            .map { filter ->
                val escapedValue = filter.value.replace("'", "''")

                when (filter.operator.uppercase()) {
                    "LIKE" -> "${filter.column} LIKE '%$escapedValue%'"
                    "IN" -> {
                        val formattedValues = escapedValue.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .joinToString(", ") { "'$it'" }

                        if (formattedValues.isNotEmpty()) {
                            "${filter.column} IN ($formattedValues)"
                        } else {
                            "1=1"
                        }
                    }
                    else -> "${filter.column} ${filter.operator} '$escapedValue'"
                }
            }

        return if (conditions.isNotEmpty()) {
            " WHERE " + conditions.joinToString(" AND ")
        } else {
            ""
        }
    }

    private fun getTypeMetaPrefs(context: Context, fileName: String) =
        context.getSharedPreferences("${fileName}__dataviewer_type_meta", Context.MODE_PRIVATE)

    private fun runtimeTypeName(value: Any?): String {
        return when (value) {
            is String -> "String"
            is Int -> "Int"
            is Long -> "Long"
            is Float -> "Float"
            is Boolean -> "Bool"
            is Set<*> -> "StringSet"
            else -> "Unknown"
        }
    }

    private fun valueToDisplay(value: Any?): String {
        return when (value) {
            is Set<*> -> value.joinToString(", ")
            null -> "null"
            else -> value.toString()
        }
    }

    private fun parseTypedInput(type: String, rawInput: String): Any? {
        val text = rawInput.trim()

        return when (type) {
            "String" -> rawInput
            "Int" -> text.toIntOrNull()
            "Long" -> text.toLongOrNull()
            "Float" -> text.toFloatOrNull()
            "Bool" -> when (text.lowercase()) {
                "true", "1" -> true
                "false", "0" -> false
                else -> null
            }
            "StringSet" -> {
                val items = text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (items.isEmpty()) null else items.toSet()
            }
            "Data" -> {
                if (isValidBase64(text)) rawInput else null
            }
            "Dictionary" -> {
                try {
                    JSONObject(text)
                    rawInput
                } catch (_: Exception) {
                    null
                }
            }
            "Array" -> {
                try {
                    JSONArray(text)
                    rawInput
                } catch (_: Exception) {
                    null
                }
            }
            else -> null
        }
    }

    private fun inferStringValueType(value: String): String {
        val text = value.trim()
        if (text.isEmpty()) return "String"

        if (isJsonObject(text)) return "Dictionary"
        if (isJsonArray(text)) return "Array"

        val lower = text.lowercase()
        if (lower == "true" || lower == "false" || lower == "1" || lower == "0") return "Bool"

        if (text.toIntOrNull() != null) return "Int"
        if (text.toLongOrNull() != null) return "Long"
        if (text.toFloatOrNull() != null) return "Float"

        // Do not auto-infer Data from plain strings to avoid false positives.
        // Data should come from explicit editor type metadata.
        return "String"
    }

    private fun isJsonObject(text: String): Boolean {
        return try {
            JSONObject(text)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun isJsonArray(text: String): Boolean {
        return try {
            JSONArray(text)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun isValidBase64(text: String): Boolean {
        if (text.isEmpty()) return false
        return try {
            android.util.Base64.decode(text, android.util.Base64.DEFAULT)
            true
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private fun validationError(type: String): String {
        return when (type) {
            "Int" -> "Value must be a valid integer (e.g. 10, -4)."
            "Long" -> "Value must be a valid long integer."
            "Float" -> "Value must be a valid decimal number (Float)."
            "Bool" -> "Bool accepts: true, false, 1, 0."
            "StringSet" -> "StringSet must contain at least one comma-separated item."
            "Data" -> "Data must be a valid Base64 string."
            "Dictionary" -> "Dictionary must be a valid JSON object, e.g. {\"a\":1}."
            "Array" -> "Array must be a valid JSON array, e.g. [1, \"a\", true]."
            else -> "Value does not match selected type '$type'."
        }
    }
}

data class SharedPrefEntry(
    val key: String,
    val value: String,
    val type: String
)