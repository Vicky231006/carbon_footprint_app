package com.example.theglobalcarbonfootprintproject.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.theglobalcarbonfootprintproject.ui.screens.ai.AiAssistantScreen
import com.example.theglobalcarbonfootprintproject.ui.screens.community.CommunityScreen
import com.example.theglobalcarbonfootprintproject.ui.screens.dashboard.DashboardScreen
import com.example.theglobalcarbonfootprintproject.ui.screens.history.HistoryScreen

enum class MainTab(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Filled.Home, Icons.Outlined.Home),
    HISTORY("History", Icons.Filled.History, Icons.Outlined.History),
    COMMUNITY("Community", Icons.Filled.People, Icons.Outlined.People),
    ASSISTANT("Assistant", Icons.Filled.Chat, Icons.Outlined.Chat)
}

@Composable
fun MainContainer(
    onNavigateToLog: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(MainTab.DASHBOARD) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                MainTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == tab) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                MainTab.DASHBOARD -> DashboardScreen(onNavigateToLog = onNavigateToLog, onNavigateToProfile = onNavigateToProfile)
                MainTab.HISTORY -> HistoryScreen()
                MainTab.COMMUNITY -> CommunityScreen()
                MainTab.ASSISTANT -> AiAssistantScreen()
            }
        }
    }
}
