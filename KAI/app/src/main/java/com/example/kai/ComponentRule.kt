package com.example.kai

enum class TargetType { DIALOG_TEXT, TEXT_REPLACE }

enum class Action { CLOSE, CANCELABLE }

data class ComponentRule(
    val id: Long = System.currentTimeMillis(),
    val packageName: String,
    val targetType: TargetType,
    val targetValue: String,
    val action: Action = Action.CLOSE,
    val replaceText: String = "",
    val enabled: Boolean = true,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()  // 添加这一行
)

data class RuleGroup(
    val packageName: String,
    val appName: String,
    val rules: List<ComponentRule>,
    val enabledCount: Int = rules.count { it.enabled }
)
