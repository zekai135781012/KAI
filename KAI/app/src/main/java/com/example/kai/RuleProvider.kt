package com.example.kai

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File
import java.io.FileNotFoundException

class RuleProvider : ContentProvider() {
    
    companion object {
        const val TAG = "RuleProvider"
        const val AUTHORITY = "com.example.kai.provider"
        const val PATH_RULES = "rules"
        const val PATH_FILE = "file"
        
        const val CODE_RULES = 1
        const val CODE_FILE = 2
        
        val URI_MATCHER = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, PATH_RULES, CODE_RULES)
            addURI(AUTHORITY, "$PATH_FILE/*", CODE_FILE)
        }
    }
    
    private lateinit var configFile: File
    private lateinit var configDir: File
    
    override fun onCreate(): Boolean {
        val pid = android.os.Process.myPid()
        Log.d(TAG, "Provider onCreate, pid=$pid, process=${android.os.Process.myPid()}")
        
        context?.let { ctx ->
            configDir = File(ctx.filesDir, "config")
            if (!configDir.exists()) {
                val created = configDir.mkdirs()
                Log.d(TAG, "创建配置目录: ${configDir.absolutePath}, 结果=$created")
            }
            
            configFile = File(configDir, "component_rules.json")
            if (!configFile.exists()) {
                configFile.writeText("[]")
                Log.d(TAG, "创建空规则文件: ${configFile.absolutePath}")
            }
            
            // 关键：设置世界可读权限，供 Xposed 使用
            ensureWorldReadable()
        }
        
        return true
    }
    
    private fun ensureWorldReadable() {
        if (::configFile.isInitialized && configFile.exists()) {
            try {
                // 设置文件权限为 644 (rw-r--r--)
                configFile.setReadable(true, false)   // 世界可读
                configFile.setWritable(true, true)    // 仅所有者可写
                configFile.setExecutable(false, false)
                
                // 设置目录权限为 755 (rwxr-xr-x)
                if (::configDir.isInitialized && configDir.exists()) {
                    configDir.setReadable(true, false)
                    configDir.setExecutable(true, false)
                }
                
                Log.d(TAG, "已设置世界可读权限: ${configFile.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "设置权限失败: ${e.message}")
            }
        }
    }
    
    override fun getType(uri: Uri): String? {
        return when (URI_MATCHER.match(uri)) {
            CODE_RULES -> "application/json"
            CODE_FILE -> "application/octet-stream"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }
    
    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        Log.d(TAG, "query: $uri")
        
        return when (URI_MATCHER.match(uri)) {
            CODE_RULES -> queryRules()
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }
    
    private fun queryRules(): Cursor {
        ensureWorldReadable()
        
        val cursor = MatrixCursor(arrayOf("_id", "data", "timestamp", "file_path"))
        
        val content = try {
            if (configFile.exists()) {
                configFile.readText()
            } else {
                "[]"
            }
        } catch (e: Exception) {
            Log.e(TAG, "读取文件失败: ${e.message}")
            "[]"
        }
        
        cursor.addRow(arrayOf(
            1, 
            content, 
            System.currentTimeMillis(),
            configFile.absolutePath
        ))
        
        Log.d(TAG, "返回规则数据，长度: ${content.length}")
        return cursor
    }
    
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        Log.d(TAG, "openFile: $uri, mode: $mode")
        
        return when (URI_MATCHER.match(uri)) {
            CODE_FILE, CODE_RULES -> {
                ensureWorldReadable()
                if (!configFile.exists()) {
                    throw FileNotFoundException("Config file not found")
                }
                ParcelFileDescriptor.open(configFile, ParcelFileDescriptor.MODE_READ_ONLY)
            }
            else -> throw FileNotFoundException("Invalid URI: $uri")
        }
    }
    
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        Log.d(TAG, "call: method=$method")
        return when (method) {
            "refresh" -> {
                ensureWorldReadable()
                context?.contentResolver?.notifyChange(
                    Uri.parse("content://$AUTHORITY/$PATH_RULES"), 
                    null
                )
                Bundle().apply { putBoolean("success", true) }
            }
            "ensure_readable" -> {
                ensureWorldReadable()
                Bundle().apply { putBoolean("success", true) }
            }
            else -> null
        }
    }
    
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0
}
