package com.example.kai

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException

class RuleProvider : ContentProvider() {
    
    companion object {
        const val AUTHORITY = "com.example.kai.provider"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/rules")
        const val PATH_RULES = "rules"
        const val PATH_FILE = "file"
    }
    
    override fun onCreate(): Boolean {
        ensureConfigFileExists()
        return true
    }
    
    private fun ensureConfigFileExists() {
        try {
            val configDir = File(context?.filesDir, "config")
            if (!configDir.exists()) {
                configDir.mkdirs()
            }
            val configFile = File(configDir, "component_rules.json")
            if (!configFile.exists()) {
                configFile.writeText("[]")
            }
        } catch (e: Exception) { }
    }
    
    override fun getType(uri: Uri): String? {
        return when (uri.pathSegments?.firstOrNull()) {
            PATH_RULES -> "application/json"
            PATH_FILE -> "application/octet-stream"
            else -> null
        }
    }
    
    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        return when (uri.pathSegments?.firstOrNull()) {
            PATH_RULES -> queryRules()
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }
    
    private fun queryRules(): Cursor {
        val cursor = MatrixCursor(arrayOf("_id", "data", "timestamp"))
        val file = getConfigFile()
        
        val content = if (file.exists()) {
            try {
                file.readText()
            } catch (e: Exception) {
                "[]"
            }
        } else {
            "[]"
        }
        
        cursor.addRow(arrayOf(1, content, System.currentTimeMillis()))
        return cursor
    }
    
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        return when (uri.pathSegments?.firstOrNull()) {
            PATH_RULES, PATH_FILE -> openConfigFile(mode)
            else -> throw FileNotFoundException("Invalid URI: $uri")
        }
    }
    
    private fun openConfigFile(mode: String): ParcelFileDescriptor? {
        val file = getConfigFile()
        if (!file.exists()) {
            ensureConfigFileExists()
        }
        if (mode != "r" && !mode.contains("r")) {
            throw SecurityException("只支持读取模式")
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }
    
    private fun getConfigFile(): File {
        return File(context?.filesDir, "config/component_rules.json")
    }
    
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0
}
