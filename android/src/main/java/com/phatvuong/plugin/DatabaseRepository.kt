package com.phatvuong.plugin

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

class DatabaseRepository private constructor() {
    companion object {
        @Volatile
        private var instance: DatabaseRepository? = null

        fun getInstance() = instance ?: synchronized(this) {
            instance ?: DatabaseRepository().also { instance = it }
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

    fun getTableData(context: Context, dbName: String, tableName: String, page: Int, pageSize: Int) : Pair<List<String>, List<List<String>>> {
        val columns = mutableListOf<String>()
        val rows = mutableListOf<List<String>>()
        val dbFile = context.getDatabasePath(dbName)

        SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("PRAGMA table_info($tableName)", null).use { cursor ->
                while (cursor.moveToNext()) {
                    columns.add(cursor.getString(1))
                }
            }

            val offset = page * pageSize
            db.rawQuery("SELECT * FROM $tableName LIMIT $pageSize OFFSET $offset", null).use { cursor ->
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
}