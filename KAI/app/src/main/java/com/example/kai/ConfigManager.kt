package com.example.kai

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

object ConfigManager {
    
    private const val TAG = "ConfigManager"
    private const val RULES_FILE = "component_rules.json"
    private const val CACHE_VALID_MS = 5000
    
    @Volatile
    private var rulesCache: List<ComponentRule> = emptyList()
    private var lastLoadTime = 0L
    private var appContext: Context? = null
    private var isXposedMode = false
    
    // Xposed 模式下缓存 Context
    private var xposedContext: Context? = null
    
    /**
     * 获取私有配置目录（应用内部，无需权限）
     */
    fun getPrivateConfigDir(): File {
        return File(appContext?.filesDir, "config")
    }
    
    private fun log(message: String) {
        Log.d(TAG, message)
        try {
            val xposedBridge = Class.forName("de.robv.android.xposed.XposedBridge")
            val logMethod = xposedBridge.getMethod("log", String::class.java)
            logMethod.invoke(null, "$TAG: $message")
        } catch (e: Exception) { }
    }
    
    /**
     * 初始化（应用内调用）
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        isXposedMode = false
        log("ConfigManager 初始化（应用模式）")
        
        val privateDir = getPrivateConfigDir()
        if (!privateDir.exists()) {
            privateDir.mkdirs()
            log("创建私有目录: ${privateDir.absolutePath}")
        }
        
        // 确保 ContentProvider 可用
        ensureProviderReady()
        
        debugInfo()
    }
    
    /**
     * Xposed 模式初始化（带 Context 缓存）
     */
    fun initForXposed(context: Context? = null) {
        isXposedMode = true
        xposedContext = context
        log("ConfigManager 初始化（Xposed模式）, context=${context != null}")
        debugInfo()
    }
    
    /**
     * 设置 Xposed 上下文（在 Hook 中获取到 Context 后调用）
     */
    fun setXposedContext(context: Context) {
        xposedContext = context
        log("Xposed Context 已更新")
    }
    
    /**
     * 确保 ContentProvider 初始化
     */
    private fun ensureProviderReady() {
        try {
            appContext?.let { ctx ->
                val uri = Uri.parse("content://com.example.kai.provider/rules")
                ctx.contentResolver.query(uri, null, null, null, null)?.close()
                log("ContentProvider 就绪")
            }
        } catch (e: Exception) {
            log("ContentProvider 检查失败: ${e.message}")
        }
    }
    
    /**
     * 获取 ContentResolver（Xposed 模式下多种方式尝试）
     */
    private fun getContentResolver(): ContentResolver? {
        // 方式1：使用缓存的 Context
        xposedContext?.let {
            return it.contentResolver
        }
        
        // 方式2：通过 ActivityThread 获取（主进程有效）
        try {
            val activityThread = Class.forName("android.app.ActivityThread")
            val currentApplication = activityThread.getMethod("currentApplication")
                .invoke(null) as? android.app.Application
            
            currentApplication?.contentResolver?.let {
                return it
            }
        } catch (e: Exception) {
            log("ActivityThread 获取失败: ${e.message}")
        }
        
        // 方式3：通过系统服务获取（最后尝试）
        try {
            val systemContext = Class.forName("android.app.ActivityThread")
                .getMethod("getSystemContext")
                .invoke(null) as? Context
            
            systemContext?.contentResolver?.let {
                log("通过 SystemContext 获取 ContentResolver")
                return it
            }
        } catch (e: Exception) {
            log("SystemContext 获取失败: ${e.message}")
        }
        
        return null
    }
    
    /**
     * 加载所有规则（Xposed 模式使用 ContentProvider 优先）
     */
    @Synchronized
    fun loadRules(forceReload: Boolean = false): List<ComponentRule> {
        val now = System.currentTimeMillis()
        if (!forceReload && now - lastLoadTime < CACHE_VALID_MS && rulesCache.isNotEmpty()) {
            return rulesCache
        }
        
        val json = if (isXposedMode) {
            loadRulesXposed()
        } else {
            loadFromPrivateFile()
        }
        
        if (json.isNullOrEmpty()) {
            log("无规则数据")
            return emptyList()
        }
        
        return try {
            parseRulesJson(json).also {
                log("加载 ${it.size} 条规则")
                rulesCache = it
                lastLoadTime = now
            }
        } catch (e: Exception) {
            log("解析失败: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Xposed 模式下加载规则（多种方式尝试）
     */
    private fun loadRulesXposed(): String? {
        // 1. 优先：ContentProvider
        loadFromContentProvider()?.let {
            log("通过 ContentProvider 加载成功")
            return it
        }
        
        // 2. 备选：直接访问模块私有目录
        val modulePrivateFile = File("/data/data/com.example.kai/files/config/$RULES_FILE")
        if (modulePrivateFile.exists()) {
            try {
                modulePrivateFile.readText().also {
                    log("通过模块私有目录加载成功")
                    return it
                }
            } catch (e: Exception) {
                log("模块私有目录读取失败: ${e.message}")
            }
        }
        
        // 3. 最后尝试：使用缓存的规则（如果之前有加载过）
        if (rulesCache.isNotEmpty()) {
            log("使用缓存的规则")
            return rulesToJson(rulesCache)
        }
        
        log("所有路径均加载失败")
        return null
    }
    
    /**
     * 通过 ContentProvider 读取
     */
    private fun loadFromContentProvider(): String? {
        return try {
            val uri = Uri.parse("content://com.example.kai.provider/rules")
            
            val contentResolver = getContentResolver()
                ?: return null.also { log("无法获取 ContentResolver") }
            
            // 方式1：查询
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val dataIndex = cursor.getColumnIndex("data")
                    if (dataIndex >= 0) {
                        return cursor.getString(dataIndex)
                    }
                }
                null
            } ?: run {
                // 方式2：打开文件描述符
                try {
                    contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        FileInputStream(pfd.fileDescriptor).use { fis ->
                            fis.bufferedReader().use { it.readText() }
                        }
                    }
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            log("ContentProvider 读取失败: ${e.message}")
            null
        }
    }
    
    /**
     * 从私有文件读取（应用模式）
     */
    private fun loadFromPrivateFile(): String? {
        val file = File(getPrivateConfigDir(), RULES_FILE)
        return if (file.exists()) {
            try {
                file.readText()
            } catch (e: Exception) {
                null
            }
        } else null
    }
    
    /**
     * 保存所有规则（仅保存到私有目录）
     */
    fun saveRules(rules: List<ComponentRule>): Boolean {
        return try {
            val json = rulesToJson(rules)
            
            val privateFile = File(getPrivateConfigDir(), RULES_FILE)
            privateFile.parentFile?.mkdirs()
            privateFile.writeText(json)
            log("保存到私有目录: ${privateFile.absolutePath}")
            
            // 通知 ContentProvider 数据变更
            notifyProviderChanged()
            
            rulesCache = rules
            lastLoadTime = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            log("保存失败: ${e.message}")
            false
        }
    }
    
    /**
     * 通知 ContentProvider 数据已变更
     */
    private fun notifyProviderChanged() {
        try {
            appContext?.contentResolver?.notifyChange(
                Uri.parse("content://com.example.kai.provider/rules"),
                null
            )
        } catch (e: Exception) { }
    }
    
    fun addRule(rule: ComponentRule): Boolean {
        val rules = loadRules().toMutableList()
        if (rules.any { 
            it.packageName == rule.packageName && 
            it.targetType == rule.targetType && 
            it.targetValue == rule.targetValue 
        }) {
            log("规则已存在")
            return false
        }
        rules.add(rule)
        return saveRules(rules)
    }
    
    fun deleteRule(ruleId: Long): Boolean {
        val rules = loadRules().filter { it.id != ruleId }
        return saveRules(rules)
    }
    
    fun updateRule(updatedRule: ComponentRule): Boolean {
        val rules = loadRules().map { 
            if (it.id == updatedRule.id) updatedRule else it 
        }
        return saveRules(rules)
    }
    
    fun toggleRule(ruleId: Long, enabled: Boolean): Boolean {
        val rules = loadRules().map { 
            if (it.id == ruleId) it.copy(enabled = enabled) else it 
        }
        return saveRules(rules)
    }
    
    fun getRulesForPackage(packageName: String): List<ComponentRule> {
        return loadRules().filter { it.packageName == packageName }
    }
    
    fun getRulesGrouped(): List<RuleGroup> {
        val rules = loadRules()
        return rules.groupBy { it.packageName }
            .map { (pkg, pkgRules) ->
                RuleGroup(
                    packageName = pkg,
                    appName = pkg,
                    rules = pkgRules,
                    enabledCount = pkgRules.count { it.enabled }
                )
            }
            .sortedByDescending { it.rules.size }
    }
    
    fun getBackups(): List<File> {
        val backupDir = File(getPrivateConfigDir(), "backup")
        if (!backupDir.exists()) return emptyList()
        return backupDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?.toList() ?: emptyList()
    }
    
    fun createBackup(): File? {
        return try {
            val rules = loadRules()
            if (rules.isEmpty()) return null
            
            val backupDir = File(getPrivateConfigDir(), "backup")
            backupDir.mkdirs()
            
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                .format(java.util.Date())
            val backupFile = File(backupDir, "backup_$timestamp.json")
            backupFile.writeText(rulesToJson(rules))
            backupFile
        } catch (e: Exception) {
            null
        }
    }
    
    fun restoreBackup(backupFile: File): Boolean {
        return try {
            val json = backupFile.readText()
            val rules = parseRulesJson(json)
            saveRules(rules)
        } catch (e: Exception) {
            false
        }
    }
    
    fun importRules(file: File): Boolean {
        return try {
            val json = file.readText()
            val rules = parseRulesJson(json)
            val existing = loadRules().toMutableList()
            rules.forEach { newRule ->
                if (existing.none { it.id == newRule.id }) {
                    existing.add(newRule)
                }
            }
            saveRules(existing)
        } catch (e: Exception) {
            false
        }
    }
    
    fun exportRules(destFile: File): Boolean {
        return try {
            val rules = loadRules()
            destFile.writeText(rulesToJson(rules))
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun clearAllRules(): Boolean {
        return saveRules(emptyList())
    }
    
    fun getRulesFilePath(): String {
        return File(getPrivateConfigDir(), RULES_FILE).absolutePath
    }
    
    fun getRulesJson(): String {
        return rulesToJson(loadRules())
    }
    
    fun debugInfo() {
        val rules = loadRules()
        log("=== ConfigManager 调试信息 ===")
        log("模式: ${if (isXposedMode) "Xposed模式" else "应用模式"}")
        log("Android 版本: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        log("私有目录: ${getPrivateConfigDir().absolutePath}")
        log("规则数量: ${rules.size}")
        rules.forEach { rule ->
            log("  - [${rule.targetType}] ${rule.packageName}: ${rule.targetValue.take(20)}...")
        }
        log("==============================")
    }
    
    private fun parseRulesJson(json: String): List<ComponentRule> {
        val rules = mutableListOf<ComponentRule>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                try {
                    rules.add(ComponentRule(
                        id = obj.optLong("id", System.currentTimeMillis()),
                        packageName = obj.optString("packageName", ""),
                        targetType = try {
                            TargetType.valueOf(obj.optString("targetType", "DIALOG_TEXT"))
                        } catch (e: Exception) {
                            TargetType.DIALOG_TEXT
                        },
                        targetValue = obj.optString("targetValue", ""),
                        action = try {
                            Action.valueOf(obj.optString("action", "CLOSE"))
                        } catch (e: Exception) {
                            Action.CLOSE
                        },
                        enabled = obj.optBoolean("enabled", true),
                        description = obj.optString("description", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    ))
                } catch (e: Exception) {
                    log("解析单条规则失败: ${e.message}")
                }
            }
        } catch (e: Exception) {
            log("解析 JSON 失败: ${e.message}")
        }
        return rules
    }
    
    private fun rulesToJson(rules: List<ComponentRule>): String {
        val arr = JSONArray()
        rules.forEach { rule ->
            arr.put(JSONObject().apply {
                put("id", rule.id)
                put("packageName", rule.packageName)
                put("targetType", rule.targetType.name)
                put("targetValue", rule.targetValue)
                put("action", rule.action.name)
                put("enabled", rule.enabled)
                put("description", rule.description)
                put("createdAt", rule.createdAt)
            })
        }
        return arr.toString(2)
    }
}