package com.example.kai

import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

object DialogBlocker {
    
    private val packageRulesCache = ConcurrentHashMap<String, List<DialogRule>>()
    
    data class DialogRule(
        val keyword: String,
        val pattern: Pattern?,
        val action: Action
    )
    
    @JvmStatic
    fun handleLoadPackage(lp: XC_LoadPackage.LoadPackageParam) {
        val pkg = lp.packageName
        
        // 跳过系统应用和自身
        if (pkg.startsWith("android.") || 
            pkg.startsWith("com.android.") || 
            pkg == "com.example.kai") {
            return
        }
        
        try {
            // 从 ConfigManager 加载规则
            val rules = ConfigManager.loadRules()
                .filter { it.packageName == pkg && it.enabled && it.targetType == TargetType.DIALOG_TEXT }
            
            if (rules.isEmpty()) {
                return
            }
            
            XposedBridge.log("DialogBlocker: $pkg 加载 ${rules.size} 条弹窗规则")
            
            rules.forEach { rule ->
                XposedBridge.log("  - 关键词: ${rule.targetValue} -> ${rule.action}")
            }
            
            val dialogRules = rules.map { rule ->
                DialogRule(
                    keyword = rule.targetValue,
                    pattern = try { Pattern.compile(rule.targetValue, Pattern.CASE_INSENSITIVE) } catch (e: Exception) { null },
                    action = rule.action
                )
            }
            packageRulesCache[pkg] = dialogRules
            
            // Hook Dialog.show() 方法
            hookDialogShow(dialogRules)
            
            // Hook Dialog.setCancelable() 方法
            hookDialogCancelable(dialogRules)
            
        } catch (e: Throwable) {
            XposedBridge.log("DialogBlocker: $pkg 错误 - ${e.message}")
        }
    }
    
    private fun hookDialogShow(rules: List<DialogRule>) {
        XposedHelpers.findAndHookMethod(
            Dialog::class.java,
            "show",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val dialog = param.thisObject as Dialog
                    
                    try {
                        val textList = getAllDialogText(dialog)
                        if (textList.isEmpty()) return
                        
                        rules.forEach { rule ->
                            val matched = textList.any { text ->
                                if (rule.pattern != null) {
                                    rule.pattern.matcher(text).find()
                                } else {
                                    text.contains(rule.keyword, ignoreCase = true)
                                }
                            }
                            
                            if (matched) {
                                XposedBridge.log("DialogBlocker: 检测到匹配弹窗 - 关键词: ${rule.keyword}")
                                
                                when (rule.action) {
                                    Action.CLOSE -> {
                                        dialog.dismiss()
                                        XposedBridge.log("DialogBlocker: 弹窗已关闭")
                                    }
                                    Action.CANCELABLE -> {
                                        dialog.setCancelable(true)
                                        XposedBridge.log("DialogBlocker: 弹窗已设置为可取消")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        XposedBridge.log("DialogBlocker: 弹窗检测失败 - ${e.message}")
                    }
                }
            }
        )
    }
    
    private fun hookDialogCancelable(rules: List<DialogRule>) {
        XposedHelpers.findAndHookMethod(
            Dialog::class.java,
            "setCancelable",
            Boolean::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val dialog = param.thisObject as Dialog
                    val setCancelable = param.args[0] as Boolean
                    
                    if (setCancelable) return
                    
                    try {
                        val textList = getAllDialogText(dialog)
                        if (textList.isEmpty()) return
                        
                        rules.forEach { rule ->
                            val matched = textList.any { text ->
                                if (rule.pattern != null) {
                                    rule.pattern.matcher(text).find()
                                } else {
                                    text.contains(rule.keyword, ignoreCase = true)
                                }
                            }
                            
                            if (matched && rule.action == Action.CANCELABLE) {
                                param.args[0] = true
                                XposedBridge.log("DialogBlocker: 强制设置弹窗可取消 - 关键词: ${rule.keyword}")
                            }
                        }
                    } catch (e: Exception) {
                        XposedBridge.log("DialogBlocker: 设置可取消失败 - ${e.message}")
                    }
                }
            }
        )
    }
    
    private fun getAllDialogText(dialog: Dialog): List<String> {
        val textList = mutableListOf<String>()
        try {
            val decorView = dialog.window?.decorView
            if (decorView != null) {
                collectTextViewText(decorView, textList)
            }
        } catch (e: Exception) {
            XposedBridge.log("获取弹窗文本失败: ${e.message}")
        }
        return textList
    }
    
    private fun collectTextViewText(view: View, textList: MutableList<String>) {
        if (view is TextView) {
            view.text?.toString()?.let { text ->
                if (text.isNotBlank()) {
                    textList.add(text)
                }
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                collectTextViewText(view.getChildAt(i), textList)
            }
        }
    }
    
    fun clearCache() {
        packageRulesCache.clear()
        XposedBridge.log("DialogBlocker: 缓存已清除")
    }
}