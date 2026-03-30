package com.example.kai

import android.content.Context
import android.os.Build
import android.util.Log
import de.robv.android.xposed.XposedBridge
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

object ConfigManager {
    private const val TAG = "ConfigManager"
    private const val RULES_FILE = "component_rules.json"
    
    // 关键修改：延长缓存时间到 30 秒，减少重复加载
    private const val CACHE_VALID_MS = 30000  // 30秒
    
    @Volatile
    private var rulesCache: List<ComponentRule> = emptyList()
    private var lastLoadTime = 0L
    var appContext: Context? = null
    var isXposedMode = false
    @Volatile
    private var _xposedContext: Context? = null

    val INSTANCE: ConfigManager
        get() = this

    fun init(context: Context) {
        appContext = context.applicationContext
        isXposedMode = false
        log("ConfigManager 初始化（应用模式）")
        ensureConfigDir()
        debugInfo()
    }

    fun initForXposed(context: Context? = null) {
        isXposedMode = true
        _xposedContext = context
        log("ConfigManager 初始化（Xposed模式）")
        // 预加载规则到缓存
        val rules = loadRules(true)  // 强制加载
        log("预加载完成，规则数: ${rules.size}")
    }

    fun updateXposedContext(context: Context) {
        _xposedContext = context
    }

    private fun getConfigDir(): File {
        val ctx = appContext ?: _xposedContext ?: return File("")
        return File(ctx.filesDir, "config")
    }

    fun getConfigFile(): File {
        return File(getConfigDir(), RULES_FILE)
    }

    private fun ensureConfigDir(): File {
        val dir = getConfigDir()
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun makeWorldReadable(file: File) {
        try {
            file.setReadable(true, false)
            file.setWritable(true, true)
        } catch (e: Exception) {}
    }

    @Synchronized
    fun loadRules(forceReload: Boolean = false): List<ComponentRule> {
        val now = System.currentTimeMillis()
        
        // 关键修改：减少日志，只在真正加载时打印
        if (!forceReload && now - lastLoadTime < CACHE_VALID_MS && rulesCache.isNotEmpty()) {
            // 不再打印"使用缓存规则"日志，避免日志爆炸
            return rulesCache
        }

        val json = if (isXposedMode) {
            loadRulesXposed()
        } else {
            loadFromPrivateFile()
        }
        
        if (json.isNullOrEmpty()) {
            rulesCache = emptyList()
            lastLoadTime = now
            return emptyList()
        }

        return try {
            parseRulesJson(json).also {
                // 只在规则变化或首次加载时打印
                if (rulesCache.size != it.size) {
                    log("加载 ${it.size} 条规则")
                }
                rulesCache = it
                lastLoadTime = now
            }
        } catch (e: Exception) {
            log("解析规则失败: ${e.message}")
            rulesCache = emptyList()
            lastLoadTime = now
            emptyList()
        }
    }

    // 关键修改：优化 Xposed 加载，减少重复尝试
    private fun loadRulesXposed(): String? {
        // 优先：直接读取文件（如果权限正确）
        loadFromPrivateFile()?.let { return it }
        
        // 其次：ContentProvider
        loadFromContentProvider()?.let { return it }
        
        // 最后：root
        loadWithRoot()?.let { return it }
        
        // 缓存
        if (rulesCache.isNotEmpty()) {
            return rulesToJson(rulesCache)
        }
        
        log("所有读取策略均失败")
        return null
    }

    private fun loadFromPrivateFile(): String? {
        val file = getConfigFile()
        // 只在失败时打印日志
        if (!file.exists()) return null
        
        return try {
            FileReader(file).use { fr ->
                BufferedReader(fr).use { it.readText() }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun loadFromContentProvider(): String? {
        return try {
            val context = _xposedContext ?: return null
            val uri = android.net.Uri.parse("content://com.example.kai.provider/rules")
            
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val dataIndex = cursor.getColumnIndex("data")
                    if (dataIndex >= 0) {
                        cursor.getString(dataIndex)?.takeIf { it.isNotBlank() }
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun loadWithRoot(): String? {
        return try {
            val file = getConfigFile()
            val process = Runtime.getRuntime().exec("su -c cat ${file.absolutePath}")
            val content = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            
            content.takeIf { it.isNotBlank() && it != "[]" }
        } catch (e: Exception) {
            null
        }
    }

    fun saveRules(rules: List<ComponentRule>): Boolean {
        return try {
            val json = rulesToJson(rules)
            val dir = ensureConfigDir()
            val file = getConfigFile()
            
            FileWriter(file).use { it.write(json) }
            makeWorldReadable(file)
            
            log("保存成功，规则数: ${rules.size}")
            
            // 清空缓存，强制下次重新加载
            lastLoadTime = 0
            
            // 通知 Provider
            try {
                appContext?.contentResolver?.notifyChange(
                    android.net.Uri.parse("content://com.example.kai.provider/rules"),
                    null
                )
            } catch (e: Exception) {}
            
            rulesCache = rules
            lastLoadTime = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            log("保存失败: ${e.message}")
            false
        }
    }

    // ========== 其他方法保持不变 ==========
    
    fun addRule(rule: ComponentRule): Boolean {
        val rules = loadRules().toMutableList()
        if (rules.any {
                it.packageName == rule.packageName &&
                it.targetType == rule.targetType &&
                it.targetValue == rule.targetValue &&
                it.targetValue.isNotBlank()
            }) {
            return false
        }
        rules.add(rule)
        return saveRules(rules)
    }

    fun deleteRule(ruleId: Long): Boolean {
        val newRules = loadRules().filter { it.id != ruleId }
        return saveRules(newRules)
    }

    fun updateRule(updatedRule: ComponentRule): Boolean {
        val newRules = loadRules().map {
            if (it.id == updatedRule.id) updatedRule else it
        }
        return saveRules(newRules)
    }

    fun toggleRule(ruleId: Long, enabled: Boolean): Boolean {
        val newRules = loadRules().map {
            if (it.id == ruleId) it.copy(enabled = enabled) else it
        }
        return saveRules(newRules)
    }

    fun getRulesForPackage(packageName: String): List<ComponentRule> {
        if (packageName.isBlank()) return emptyList()
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

    fun getAllRules(): List<ComponentRule> = loadRules()

    fun createBackup(): File? {
        return try {
            val rules = loadRules()
            if (rules.isEmpty()) return null
            val backupDir = File(getConfigDir(), "backup")
            if (!backupDir.exists() && !backupDir.mkdirs()) return null
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(backupDir, "backup_$timestamp.json")
            FileWriter(backupFile).use { it.write(rulesToJson(rules)) }
            makeWorldReadable(backupFile)
            backupFile
        } catch (e: Exception) {
            null
        }
    }

    fun restoreBackup(backupFile: File): Boolean {
        if (!backupFile.exists() || !backupFile.canRead()) return false
        return try {
            val json = FileReader(backupFile).use { BufferedReader(it).readText() }
            val rules = parseRulesJson(json)
            saveRules(rules)
        } catch (e: Exception) {
            false
        }
    }

    fun importRules(file: File): Boolean {
        if (!file.exists() || !file.canRead()) return false
        return try {
            val json = FileReader(file).use { BufferedReader(it).readText() }
            val newRules = parseRulesJson(json)
            val existingRules = loadRules().toMutableList()
            newRules.forEach { newRule ->
                if (existingRules.none { it.id == newRule.id }) {
                    existingRules.add(newRule)
                }
            }
            saveRules(existingRules)
        } catch (e: Exception) {
            false
        }
    }

    fun exportRules(destFile: File): Boolean {
        if (!destFile.parentFile?.exists()!! && !destFile.parentFile?.mkdirs()!!) return false
        return try {
            val rules = loadRules()
            FileWriter(destFile).use { it.write(rulesToJson(rules)) }
            makeWorldReadable(destFile)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun clearAllRules(): Boolean {
        return saveRules(emptyList())
    }

    fun getRulesFilePath(): String {
        return getConfigFile().absolutePath
    }

    private fun rulesToJson(rules: List<ComponentRule>): String {
        val arr = JSONArray()
        rules.forEach { rule ->
            JSONObject().apply {
                put("id", rule.id)
                put("packageName", rule.packageName ?: "")
                put("targetType", rule.targetType?.name ?: TargetType.DIALOG_TEXT.name)
                put("targetValue", rule.targetValue ?: "")
                put("action", rule.action?.name ?: Action.CLOSE.name)
                put("replaceText", rule.replaceText ?: "")
                put("enabled", rule.enabled)
                put("description", rule.description ?: "")
                put("createdAt", rule.createdAt)
            }.also { arr.put(it) }
        }
        return arr.toString(2)
    }

    private fun parseRulesJson(json: String): List<ComponentRule> {
        val rules = mutableListOf<ComponentRule>()
        if (json.isBlank()) return rules
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                try {
                    val replaceText = obj.optString("replaceText", "").trim()
                    rules.add(ComponentRule(
                        id = obj.optLong("id", System.currentTimeMillis()),
                        packageName = obj.optString("packageName", "").trim(),
                        targetType = try {
                            TargetType.valueOf(obj.optString("targetType", "DIALOG_TEXT").uppercase())
                        } catch (e: Exception) {
                            TargetType.DIALOG_TEXT
                        },
                        targetValue = obj.optString("targetValue", "").trim(),
                        action = try {
                            Action.valueOf(obj.optString("action", "CLOSE").uppercase())
                        } catch (e: Exception) {
                            Action.CLOSE
                        },
                        replaceText = replaceText,
                        enabled = obj.optBoolean("enabled", true),
                        description = obj.optString("description", "").trim(),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    ))
                } catch (e: Exception) {
                    continue
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }
        return rules
    }

    private fun log(message: String) {
        Log.d(TAG, message)
        try {
            val xposedBridge = Class.forName("de.robv.android.xposed.XposedBridge")
            val logMethod = xposedBridge.getMethod("log", String::class.java)
            logMethod.invoke(null, "$TAG: $message")
        } catch (e: Exception) {}
    }

    fun debugInfo() {
        try {
            val rules = loadRules()
            log("=== ConfigManager 调试信息 ===")
            log("模式: ${if (isXposedMode) "Xposed模式" else "应用模式"}")
            log("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            log("规则数: ${rules.size}")
            log("==============================")
        } catch (e: Exception) {}
    }
}
