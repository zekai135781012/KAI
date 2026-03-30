package com.example.kai

import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.regex.Pattern

object ComponentBlocker {

    @JvmStatic
    fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val pkg = lpparam.packageName
        XposedBridge.log("ComponentBlocker: 处理应用 $pkg")

        val allRules = ConfigManager.INSTANCE.loadRules(false)
        val appRules = allRules.filter { it.packageName == pkg && it.enabled }
        val dialogRules = appRules.filter { it.targetType == TargetType.DIALOG_TEXT }
        val textRules = appRules.filter { it.targetType == TargetType.TEXT_REPLACE }

        XposedBridge.log("ComponentBlocker: $pkg 弹窗规则 ${dialogRules.size} 条, 文本规则 ${textRules.size} 条")

        dialogRules.forEachIndexed { index, rule ->
            XposedBridge.log("ComponentBlocker: 弹窗规则[$index]: '${rule.targetValue}' -> ${rule.action}")
        }
        textRules.forEachIndexed { index, rule ->
            XposedBridge.log("ComponentBlocker: 文本规则[$index]: '${rule.targetValue}' -> '${rule.replaceText}'")
        }

        if (dialogRules.isNotEmpty()) {
            hookDialogBlock(lpparam, dialogRules)
        }
        if (textRules.isNotEmpty()) {
            hookTextViewReplace(lpparam, textRules)
        }
    }

    // ==================== 弹窗拦截 ====================
    private fun hookDialogBlock(lpparam: XC_LoadPackage.LoadPackageParam, rules: List<ComponentRule>) {
        val pkg = lpparam.packageName
        try {
            val targetRules = rules.map { rule ->
                DialogRule(
                    pattern = Pattern.compile(rule.targetValue, Pattern.CASE_INSENSITIVE),
                    action = rule.action
                )
            }
            hookDialogClass(android.app.Dialog::class.java, pkg, targetRules, "android.app.Dialog")

            val dialogSubClasses = listOf(
                "android.app.AlertDialog",
                "android.app.Presentation",
                "androidx.appcompat.app.AlertDialog",
                "androidx.appcompat.app.AppCompatDialog",
                "com.google.android.material.bottomsheet.BottomSheetDialog",
                "com.google.android.material.dialog.MaterialAlertDialog",
                "androidx.fragment.app.DialogFragment",
                "moe.shizuku.dialog.DialogFragment",
                "com.android.internal.app.AlertController"
            )

            dialogSubClasses.forEach { className ->
                try {
                    val clazz = XposedHelpers.findClass(className, lpparam.classLoader)
                    hookDialogClass(clazz, pkg, targetRules, className)
                } catch (e: Throwable) {
                }
            }
            XposedBridge.log("ComponentBlocker: $pkg 弹窗Hook已启用")
        } catch (e: Throwable) {
            XposedBridge.log("ComponentBlocker: $pkg 弹窗Hook错误: ${e.message}")
        }
    }

    private fun hookDialogClass(
        dialogClass: Class<*>, pkg: String, targetRules: List<DialogRule>, className: String
    ) {
        try {
            XposedHelpers.findAndHookMethod(dialogClass, "show", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    processDialog(param.thisObject as Dialog, pkg, targetRules, className, false)
                }
                override fun afterHookedMethod(param: MethodHookParam) {
                    processDialog(param.thisObject as Dialog, pkg, targetRules, className, true)
                }
            })
            XposedBridge.log("ComponentBlocker: $pkg Hook $className.show() 成功")
        } catch (e: Throwable) {
            XposedBridge.log("ComponentBlocker: $pkg Hook $className.show() 失败: ${e.message}")
        }
    }

    private fun processDialog(
        dialog: Dialog, pkg: String, targetRules: List<DialogRule>, className: String, isShown: Boolean
    ) {
        try {
            val window = dialog.window ?: return
            val decorView = window.decorView ?: return
            val texts = collectAllTexts(decorView)
            if (texts.isNotEmpty()) {
                XposedBridge.log("ComponentBlocker: $pkg Dialog 文本: ${texts.take(5)}")
            }
            targetRules.forEachIndexed { index, rule ->
                val matched = texts.find { rule.pattern.matcher(it).find() }
                if (matched != null) {
                    XposedBridge.log("ComponentBlocker: 匹配弹窗: $matched")
                    when (rule.action) {
                        Action.CLOSE -> if (isShown) dialog.dismiss() else dialog.cancel()
                        Action.CANCELABLE -> {
                            dialog.setCancelable(true)
                            dialog.setCanceledOnTouchOutside(true)
                        }
                        else -> Unit
                    }
                }
            }
        } catch (e: Throwable) {
            XposedBridge.log("ComponentBlocker: 处理弹窗错误: ${e.message}")
        }
    }

    // ==================== 原生文本替换 ====================
    private fun hookTextViewReplace(lpparam: XC_LoadPackage.LoadPackageParam, rules: List<ComponentRule>) {
        try {
            val textViewClass = XposedHelpers.findClass("android.widget.TextView", lpparam.classLoader)
            val compiledRules = rules.map {
                CompiledTextRule(
                    Pattern.compile(it.targetValue, Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE),
                    it.replaceText
                )
            }

            val setTextMethods = listOf(
                arrayOf(CharSequence::class.java),
                arrayOf(CharSequence::class.java, TextView.BufferType::class.java)
            )

            setTextMethods.forEach { params ->
                XposedHelpers.findAndHookMethod(textViewClass, "setText", *params, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (param.args[0] is Int) return
                        val orig = param.args[0]?.toString() ?: return
                        var mod = orig
                        var changed = false
                        compiledRules.forEach { rule ->
                            val m = rule.pattern.matcher(mod)
                            if (m.find()) {
                                mod = m.replaceAll(rule.replaceText)
                                changed = true
                            }
                        }
                        if (changed) param.args[0] = mod
                    }
                })
            }
            XposedBridge.log("ComponentBlocker: ${lpparam.packageName} 文本Hook已启用")
        } catch (e: Throwable) {
            XposedBridge.log("ComponentBlocker: 文本Hook错误: ${e.message}")
        }
    }

    private fun collectAllTexts(view: View?): List<String> = buildList {
        view ?: return@buildList
        try {
            if (view is TextView) add(view.text?.toString() ?: "")
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    addAll(collectAllTexts(view.getChildAt(i)))
                }
            }
        } catch (_: Throwable) {}
    }

    private data class DialogRule(val pattern: Pattern, val action: Action?)
    private data class CompiledTextRule(val pattern: Pattern, val replaceText: String)
}
