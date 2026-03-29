package com.example.kai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.kai.ui.theme.KAITheme

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            KAITheme {
                MainScreen()
            }
        }
        
        initApp()
    }
    
    private fun initApp() {
        ConfigManager.init(this)
        ConfigManager.debugInfo()
    }
}

data class NavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAbout by remember { mutableStateOf(false) }
    
    val navItems = listOf(
        NavItem(
            label = "首页",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home
        ),
        NavItem(
            label = "应用",
            selectedIcon = KaiIcons.Apps,
            unselectedIcon = KaiIcons.AppsOutlined
        ),
        NavItem(
            label = "拓展",
            selectedIcon = KaiIcons.Extension,
            unselectedIcon = KaiIcons.ExtensionOutlined
        )
    )
    
    BackHandler(enabled = showAbout) {
        showAbout = false
    }
    
    Scaffold(
        bottomBar = {
            if (!showAbout) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    navItems.forEachIndexed { index, item ->
                        val selected = selectedTab == index
                        NavigationBarItem(
                            icon = { 
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                ) 
                            },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = { 
                                selectedTab = index
                                showAbout = false
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Crossfade(
                targetState = Pair(selectedTab, showAbout),
                animationSpec = tween(300),
                label = "screen_transition"
            ) { (tab, about) ->
                when {
                    about -> AboutScreen(onBack = { showAbout = false })
                    tab == 0 -> HomeScreen(onShowAbout = { showAbout = true })
                    tab == 1 -> AppsListScreen()
                    tab == 2 -> ExpandScreen()
                }
            }
        }
    }
}
