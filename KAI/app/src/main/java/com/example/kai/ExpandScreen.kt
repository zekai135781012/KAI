package com.example.kai

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun ExpandScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val colors = MaterialTheme.colorScheme
    
    var ruleGroups by remember { mutableStateOf(listOf<RuleGroup>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showJsonDialog by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf<RuleGroup?>(null) }
    var jsonContent by remember { mutableStateOf("") }
    var jsonFilePath by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        ruleGroups = ConfigManager.getRulesGrouped()
        jsonContent = ConfigManager.getRulesJson()
        jsonFilePath = ConfigManager.getRulesFilePath()
    }
    
    val refreshData = {
        scope.launch {
            ruleGroups = ConfigManager.getRulesGrouped()
            jsonContent = ConfigManager.getRulesJson()
            jsonFilePath = ConfigManager.getRulesFilePath()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "弹窗屏蔽",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { showJsonDialog = true }) {
                    Icon(
                        imageVector = KaiIcons.Code,
                        contentDescription = "规则配置文件",
                        tint = colors.primary
                    )
                }
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "添加规则",
                        tint = colors.primary
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        StatisticRow(ruleGroups)
        
        Spacer(Modifier.height(12.dp))
        ThinDivider()
        Spacer(Modifier.height(12.dp))
        
        if (ruleGroups.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ruleGroups, key = { it.packageName }) { group ->
                    RuleGroupItem(
                        group = group,
                        onClick = { selectedGroup = group },
                        onToggleRule = { ruleId, enabled ->
                            ConfigManager.toggleRule(ruleId, enabled)
                            refreshData()
                        },
                        onDeleteRule = { ruleId ->
                            ConfigManager.deleteRule(ruleId)
                            refreshData()
                            scope.launch {
                                snackbarHostState.showSnackbar("规则已删除")
                            }
                        }
                    )
                }
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    
    if (showJsonDialog) {
        JsonViewerDialog(
            jsonContent = jsonContent,
            filePath = jsonFilePath,
            onDismiss = { showJsonDialog = false },
            onRefresh = { refreshData() }
        )
    }
    
    if (showAddDialog) {
        AddRuleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { rule ->
                scope.launch {
                    val success = ConfigManager.addRule(rule)
                    if (success) {
                        refreshData()
                        snackbarHostState.showSnackbar("规则添加成功")
                    } else {
                        snackbarHostState.showSnackbar("规则已存在或保存失败")
                    }
                }
                showAddDialog = false
            }
        )
    }
    
    if (selectedGroup != null) {
        RuleGroupDetailDialog(
            group = selectedGroup!!,
            onDismiss = { selectedGroup = null },
            onRuleUpdated = {
                refreshData()
                selectedGroup = null
            }
        )
    }
}

@Composable
private fun StatisticRow(groups: List<RuleGroup>) {
    val totalRules = groups.sumOf { it.rules.size }
    val enabledRules = groups.sumOf { it.enabledCount }
    val colors = MaterialTheme.colorScheme
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatisticItem("应用", groups.size.toString(), colors.primary)
        StatisticItem("规则", totalRules.toString(), colors.secondary)
        StatisticItem("启用", enabledRules.toString(), colors.tertiary)
    }
}

@Composable
private fun StatisticItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThinDivider() {
    Divider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    )
}

@Composable
private fun RuleGroupItem(
    group: RuleGroup,
    onClick: () -> Unit,
    onToggleRule: (Long, Boolean) -> Unit,
    onDeleteRule: (Long) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = group.appName.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimaryContainer
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.appName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface
                )
                Text(
                    text = "${group.enabledCount}/${group.rules.size} 条规则已启用",
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant
                )
            }
            
            IconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = "展开/收起",
                    tint = colors.onSurfaceVariant
                )
            }
        }
        
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(start = 52.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                group.rules.forEach { rule ->
                    RuleRow(
                        rule = rule,
                        onToggle = { onToggleRule(rule.id, it) },
                        onDelete = { onDeleteRule(rule.id) }
                    )
                }
            }
        }
        
        ThinDivider()
    }
}

@Composable
private fun RuleRow(
    rule: ComponentRule,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (rule.enabled) colors.primary.copy(alpha = 0.2f) else colors.surfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = if (rule.enabled) "启用" else "停用",
                        fontSize = 10.sp,
                        color = if (rule.enabled) colors.primary else colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                
                Text(
                    text = rule.targetValue.take(25),
                    fontSize = 14.sp,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (rule.description.isNotEmpty()) {
                Text(
                    text = rule.description,
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = rule.enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.scale(0.8f)
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = KaiIcons.DeleteOutline,
                    contentDescription = "删除",
                    tint = colors.error.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    val colors = MaterialTheme.colorScheme
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = KaiIcons.Block,
            contentDescription = "暂无规则",
            modifier = Modifier.size(56.dp),
            tint = colors.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无弹窗屏蔽规则",
            fontSize = 16.sp,
            color = colors.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击右上角 + 添加第一条规则",
            fontSize = 13.sp,
            color = colors.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (ComponentRule) -> Unit
) {
    var packageName by remember { mutableStateOf("") }
    var targetValue by remember { mutableStateOf("") }
    var action by remember { mutableStateOf(Action.CLOSE) }
    var description by remember { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加规则") },
        containerColor = colors.surface,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("应用包名") },
                    placeholder = { Text("com.example.app") },
                    singleLine = true,
                    leadingIcon = { Icon(imageVector = KaiIcons.Apps, contentDescription = "应用", modifier = Modifier.size(20.dp)) }
                )
                
                OutlinedTextField(
                    value = targetValue,
                    onValueChange = { targetValue = it },
                    label = { Text("屏蔽关键词") },
                    placeholder = { Text("广告|VIP|会员") },
                    singleLine = true,
                    leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = "搜索", modifier = Modifier.size(20.dp)) }
                )
                
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = when(action) {
                            Action.CLOSE -> "关闭弹窗"
                            Action.CANCELABLE -> "可取消"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("操作") },
                        leadingIcon = { Icon(imageVector = KaiIcons.TouchApp, contentDescription = "操作", modifier = Modifier.size(20.dp)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("关闭弹窗") },
                            onClick = { action = Action.CLOSE; expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("设置为可取消") },
                            onClick = { action = Action.CANCELABLE; expanded = false }
                        )
                    }
                }
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述（可选）") },
                    placeholder = { Text("屏蔽广告弹窗") },
                    singleLine = true,
                    leadingIcon = { Icon(imageVector = KaiIcons.EditNote, contentDescription = "描述", modifier = Modifier.size(20.dp)) }
                )
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "💡 提示",
                            fontSize = 12.sp,
                            color = colors.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "• 正则支持：\"广告|VIP\" 匹配任一关键词\n• 不区分大小写\n• 关闭弹窗：直接关闭匹配弹窗",
                            fontSize = 11.sp,
                            color = colors.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (packageName.isNotBlank() && targetValue.isNotBlank()) {
                        onConfirm(ComponentRule(
                            packageName = packageName.trim(),
                            targetType = TargetType.DIALOG_TEXT,
                            targetValue = targetValue.trim(),
                            action = action,
                            description = description
                        ))
                    }
                },
                enabled = packageName.isNotBlank() && targetValue.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleGroupDetailDialog(
    group: RuleGroup,
    onDismiss: () -> Unit,
    onRuleUpdated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var rules by remember { mutableStateOf(group.rules) }
    val colors = MaterialTheme.colorScheme
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(group.appName)
                TextButton(
                    onClick = {
                        ConfigManager.clearAllRules()
                        rules = emptyList()
                        onRuleUpdated()
                        onDismiss()
                    }
                ) {
                    Text("清空全部", color = colors.error)
                }
            }
        },
        containerColor = colors.surface,
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rules, key = { it.id }) { rule ->
                    var editing by remember { mutableStateOf(false) }
                    
                    if (editing) {
                        RuleEditorCard(
                            rule = rule,
                            onSave = { updated ->
                                ConfigManager.updateRule(updated)
                                rules = rules.map { if (it.id == updated.id) updated else it }
                                editing = false
                                onRuleUpdated()
                            },
                            onCancel = { editing = false }
                        )
                    } else {
                        RuleDetailRow(
                            rule = rule,
                            onEdit = { editing = true },
                            onDelete = {
                                scope.launch {
                                    ConfigManager.deleteRule(rule.id)
                                    rules = rules.filter { it.id != rule.id }
                                    onRuleUpdated()
                                }
                            },
                            onToggle = { enabled ->
                                ConfigManager.toggleRule(rule.id, enabled)
                                rules = rules.map { 
                                    if (it.id == rule.id) it.copy(enabled = enabled) else it 
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun RuleDetailRow(
    rule: ComponentRule,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = colors.primaryContainer,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        "关键词",
                        fontSize = 10.sp,
                        color = colors.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = rule.targetValue,
                    fontSize = 15.sp,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Switch(
                checked = rule.enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.scale(0.8f)
            )
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = colors.secondaryContainer
            ) {
                Text(
                    text = if (rule.action == Action.CLOSE) "关闭弹窗" else "可取消",
                    fontSize = 11.sp,
                    color = colors.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Filled.Edit, contentDescription = "编辑", modifier = Modifier.size(18.dp), tint = colors.primary)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "删除", modifier = Modifier.size(18.dp), tint = colors.error)
                }
            }
        }
        
        if (rule.description.isNotEmpty()) {
            Text(
                text = rule.description,
                fontSize = 12.sp,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        ThinDivider()
    }
}

@Composable
private fun RuleEditorCard(
    rule: ComponentRule,
    onSave: (ComponentRule) -> Unit,
    onCancel: () -> Unit
) {
    var targetValue by remember { mutableStateOf(rule.targetValue) }
    var description by remember { mutableStateOf(rule.description) }
    var action by remember { mutableStateOf(rule.action) }
    val colors = MaterialTheme.colorScheme
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.secondaryContainer.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            OutlinedTextField(
                value = targetValue,
                onValueChange = { targetValue = it },
                label = { Text("屏蔽关键词") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                OutlinedTextField(
                    value = when(action) {
                        Action.CLOSE -> "关闭弹窗"
                        Action.CANCELABLE -> "设置为可取消"
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("操作") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("关闭弹窗") },
                        onClick = { action = Action.CLOSE; expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("设置为可取消") },
                        onClick = { action = Action.CANCELABLE; expanded = false }
                    )
                }
            }
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("描述") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCancel) { Text("取消") }
                TextButton(
                    onClick = {
                        onSave(rule.copy(
                            targetValue = targetValue,
                            action = action,
                            description = description
                        ))
                    }
                ) { Text("保存") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JsonViewerDialog(
    jsonContent: String,
    filePath: String,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val colors = MaterialTheme.colorScheme
    var isFormatted by remember { mutableStateOf(true) }
    var displayContent by remember { mutableStateOf(jsonContent) }
    var showToastMessage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(showToastMessage) {
        showToastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            showToastMessage = null
        }
    }
    
    fun formatJson(json: String): String {
        return try {
            org.json.JSONArray(json).toString(2)
        } catch (e: Exception) {
            try {
                org.json.JSONObject(json).toString(2)
            } catch (e2: Exception) {
                json
            }
        }
    }
    
    fun compactJson(json: String): String {
        return try {
            org.json.JSONArray(json).toString()
        } catch (e: Exception) {
            try {
                org.json.JSONObject(json).toString()
            } catch (e2: Exception) {
                json
            }
        }
    }
    
    LaunchedEffect(jsonContent, isFormatted) {
        displayContent = if (isFormatted) formatJson(jsonContent) else compactJson(jsonContent)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("规则配置")
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colors.primaryContainer
                ) {
                    Text(
                        text = if (isFormatted) "格式化" else "压缩",
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        containerColor = colors.surface,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "📁 路径",
                            fontSize = 12.sp,
                            color = colors.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = filePath,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = colors.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.primaryContainer
                    ) {
                        Text(
                            "${jsonContent.length} 字符",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    Row {
                        IconButton(
                            onClick = { isFormatted = !isFormatted },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isFormatted) KaiIcons.FormatAlignLeft else KaiIcons.FormatAlignJustify,
                                contentDescription = "格式切换",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(displayContent))
                                showToastMessage = "已复制"
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = KaiIcons.ContentCopy, contentDescription = "复制", modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = {
                                onRefresh()
                                showToastMessage = "已刷新"
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Refresh, contentDescription = "刷新", modifier = Modifier.size(20.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (displayContent.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无规则", color = colors.onSurfaceVariant)
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .padding(12.dp)
                        ) {
                            item {
                                Text(
                                    text = displayContent,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.onSurface,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}
