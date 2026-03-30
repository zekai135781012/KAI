package com.example.kai

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UserAppInfo(
    val pkg: String,
    val name: String,
    val icon: Bitmap?,
    val version: String,
    val installTime: Long
)

fun Drawable.safeToBitmap(size: Int = 128): Bitmap? {
    return try {
        val targetSize = if (size <= 0) 128 else size
        val bmp = if (this is BitmapDrawable) {
            this.bitmap.takeIf { it.width > 0 && it.height > 0 } ?: return null
        } else {
            Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888).apply {
                Canvas(this).apply {
                    setBounds(0, 0, targetSize, targetSize)
                    draw(this)
                }
            }
        }
        bmp
    } catch (e: Exception) {
        null
    }
}

@Composable
fun ExpandScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var apps by remember { mutableStateOf(emptyList<UserAppInfo>()) }
    var selected by remember { mutableStateOf<UserAppInfo?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }

    BackHandler(enabled = selected != null) {
        selected = null
    }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            var appList = emptyList<UserAppInfo>()
            try {
                val pm = ctx.packageManager
                appList = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 }
                    .mapNotNull { app ->
                        try {
                            val pkgInfo = pm.getPackageInfo(app.packageName, 0)
                            UserAppInfo(
                                pkg = app.packageName,
                                name = pm.getApplicationLabel(app).toString().takeIf { it.isNotBlank() } ?: app.packageName,
                                icon = app.loadIcon(pm).safeToBitmap(),
                                version = pkgInfo.versionName ?: "未知版本",
                                installTime = pkgInfo.firstInstallTime
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    .sortedBy { it.name }
            } catch (e: Exception) {
                loadError = true
            } finally {
                withContext(Dispatchers.Main) {
                    apps = appList
                    loading = false
                }
            }
        }
    }

    Crossfade(
        targetState = selected,
        animationSpec = tween(300),
        label = "app_detail_transition"
    ) { targetApp ->
        if (targetApp != null) {
            AppDetail(app = targetApp, onBack = { selected = null })
        } else {
            AppList(
                apps = apps,
                loading = loading,
                loadError = loadError,
                onSelect = { selected = it }
            )
        }
    }
}

@Composable
private fun AppList(apps: List<UserAppInfo>, loading: Boolean, loadError: Boolean, onSelect: (UserAppInfo) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var search by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }

    val filtered = remember(apps, search) {
        if (search.isBlank()) apps else apps.filter {
            it.name.contains(search, true) || it.pkg.contains(search, true)
        }
    }

    Column(Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            if (searching) {
                IconButton(onClick = { searching = false; search = "" }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = colors.primary)
                }
                TextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    placeholder = { Text("搜索") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
            } else {
                Text("应用管理", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.onBackground)
                IconButton(onClick = { searching = true }) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = colors.primary)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Divider(thickness = 0.5.dp, color = colors.onSurfaceVariant.copy(0.2f))
        Spacer(Modifier.height(12.dp))

        when {
            loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
            loadError -> Empty(text = "加载失败，请检查权限")
            filtered.isEmpty() -> Empty(text = "暂无应用")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(filtered, key = { it.pkg }) { AppItem(it, onSelect) }
            }
        }
    }
}

@Composable
private fun AppItem(app: UserAppInfo, onClick: (UserAppInfo) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().clickable { onClick(app) }) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AppIcon(icon = app.icon, name = app.name, size = 56)
            Column(Modifier.weight(1f)) {
                Text(
                    app.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    app.pkg,
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "v${app.version}",
                    fontSize = 11.sp,
                    color = colors.onSurfaceVariant.copy(0.7f)
                )
            }
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.onSurfaceVariant.copy(0.5f))
        }
        Divider(Modifier.padding(start = 70.dp), thickness = 0.5.dp, color = colors.onSurfaceVariant.copy(0.2f))
    }
}

@Composable
private fun AppDetail(app: UserAppInfo, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var tab by remember { mutableIntStateOf(0) }
    var refresh by remember { mutableIntStateOf(0) }

    val rules = remember(app.pkg, refresh) {
        try {
            ConfigManager.getRulesForPackage(app.pkg)
                .partition { it.targetType == TargetType.DIALOG_TEXT }
        } catch (e: Exception) {
            emptyList<ComponentRule>() to emptyList<ComponentRule>()
        }
    }
    val (dialogRules, textRules) = rules

    Column(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onBack)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = colors.primary)
            Spacer(Modifier.width(8.dp))
            Text("返回", fontSize = 16.sp, color = colors.primary)
        }

        Spacer(Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.secondaryContainer.copy(0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(icon = app.icon, name = app.name, size = 64)
                Column {
                    Text(
                        app.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                    Text(app.pkg, fontSize = 13.sp, color = colors.onSurfaceVariant)
                    Text(
                        "v${app.version}",
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariant.copy(0.8f)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        TabRow(
            selectedTabIndex = tab,
            modifier = Modifier.fillMaxWidth(),
            containerColor = colors.surface,
            contentColor = colors.primary
        ) {
            Tab(
                selected = tab == 0,
                onClick = { tab = 0 }
            ) {
                TabContent(
                    icon = Icons.Filled.Close,
                    label = "弹窗屏蔽",
                    count = dialogRules.size,
                    selected = tab == 0
                )
            }
            Tab(
                selected = tab == 1,
                onClick = { tab = 1 }
            ) {
                TabContent(
                    icon = Icons.Filled.Edit,
                    label = "文本替换",
                    count = textRules.size,
                    selected = tab == 1
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        when (tab) {
            0 -> RuleList(
                pkg = app.pkg,
                type = TargetType.DIALOG_TEXT,
                rules = dialogRules,
                onChange = { refresh++ }
            )
            1 -> RuleList(
                pkg = app.pkg,
                type = TargetType.TEXT_REPLACE,
                rules = textRules,
                onChange = { refresh++ }
            )
        }
    }
}

@Composable
private fun TabContent(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, count: Int, selected: Boolean) {
    val color = if (selected) LocalContentColor.current else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = color)
        Spacer(Modifier.width(6.dp))
        Text("$label ($count)", color = color)
    }
}

@Composable
private fun RuleList(pkg: String, type: TargetType, rules: List<ComponentRule>, onChange: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var showAdd by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Button(
            onClick = { showAdd = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (type == TargetType.TEXT_REPLACE) colors.tertiaryContainer else colors.primaryContainer,
                contentColor = if (type == TargetType.TEXT_REPLACE) colors.onTertiaryContainer else colors.onPrimaryContainer
            )
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (type == TargetType.DIALOG_TEXT) "添加弹窗规则" else "添加替换规则")
        }

        Spacer(Modifier.height(12.dp))

        if (rules.isEmpty()) {
            Empty(if (type == TargetType.DIALOG_TEXT) "暂无弹窗规则" else "暂无替换规则")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(rules, key = { it.id }) { RuleCard(rule = it, type = type, onChange = onChange) }
            }
        }
    }

    if (showAdd) AddRuleDialog(pkg = pkg, type = type, onDismiss = { showAdd = false }, onAdd = onChange)
}

@Composable
private fun RuleCard(rule: ComponentRule, type: TargetType, onChange: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val isText = type == TargetType.TEXT_REPLACE
    val activeColor = if (isText) colors.tertiary else colors.primary

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isText) colors.tertiaryContainer.copy(0.3f) else colors.surfaceVariant.copy(0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    rule.targetValue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (rule.enabled) activeColor.copy(0.15f) else colors.surfaceVariant
                    ) {
                        Text(
                            if (rule.enabled) "已启用" else "已停用",
                            fontSize = 11.sp,
                            color = if (rule.enabled) activeColor else colors.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    val actionText = when {
                        isText -> "替换为: ${rule.replaceText}"
                        rule.action == Action.CLOSE -> "自动关闭"
                        else -> "可取消"
                    }

                    Text(
                        actionText,
                        fontSize = 13.sp,
                        color = if (isText) activeColor else colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = {
                        ConfigManager.toggleRule(rule.id, it)
                        onChange()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = activeColor,
                        checkedTrackColor = activeColor.copy(0.5f)
                    )
                )

                IconButton(
                    onClick = {
                        ConfigManager.deleteRule(rule.id)
                        onChange()
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "删除",
                        tint = colors.error.copy(0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRuleDialog(pkg: String, type: TargetType, onDismiss: () -> Unit, onAdd: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var target by remember { mutableStateOf("") }
    var replace by remember { mutableStateOf("") }
    var action by remember { mutableStateOf(Action.CLOSE) }
    var expanded by remember { mutableStateOf(false) }

    val isText = type == TargetType.TEXT_REPLACE

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isText) "添加文字替换" else "添加弹窗屏蔽",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        containerColor = colors.surface,
        shape = RoundedCornerShape(28.dp),
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("匹配文本") },
                    placeholder = { Text("输入要匹配的文本或正则") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.outline
                    )
                )

                if (isText) {
                    OutlinedTextField(
                        value = replace,
                        onValueChange = { replace = it },
                        label = { Text("替换为") },
                        placeholder = { Text("输入替换后的文本") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.tertiary,
                            unfocusedBorderColor = colors.outline
                        )
                    )
                } else {
                    Text(
                        "操作类型",
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = when(action) {
                                Action.CLOSE -> "关闭弹窗"
                                else -> "可取消"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("选择操作") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("关闭弹窗") },
                                onClick = {
                                    action = Action.CLOSE
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("可取消") },
                                onClick = {
                                    action = Action.CANCELABLE
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val rule = ComponentRule(
                        packageName = pkg,
                        targetType = type,
                        targetValue = target.trim(),
                        action = action,
                        replaceText = replace.trim(),
                        description = ""
                    )
                    ConfigManager.addRule(rule)
                    onAdd(); onDismiss()
                },
                enabled = target.isNotBlank() && (!isText || replace.isNotBlank())
            ) {
                Text("添加", fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = colors.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun AppIcon(icon: Bitmap?, name: String, size: Int = 56) {
    val colors = MaterialTheme.colorScheme
    val dp = size.dp
    if (icon != null) {
        Image(
            painter = BitmapPainter(icon.asImageBitmap()),
            contentDescription = null,
            modifier = Modifier
                .size(dp)
                .clip(RoundedCornerShape((size / 4).dp))
        )
    } else {
        Surface(
            modifier = Modifier.size(dp),
            shape = RoundedCornerShape((size / 4).dp),
            color = colors.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    name.firstOrNull()?.uppercase() ?: "?",
                    fontSize = (size / 2).sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun Empty(text: String) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = colors.onSurfaceVariant.copy(0.4f)
            )
            Spacer(Modifier.height(12.dp))
            Text(text, color = colors.onSurfaceVariant)
        }
    }
}
