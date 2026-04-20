package com.example.theglobalcarbonfootprintproject.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.theglobalcarbonfootprintproject.ui.screens.dashboard.DashboardScreen
import com.example.theglobalcarbonfootprintproject.ui.screens.history.HistoryScreen
import com.example.theglobalcarbonfootprintproject.ui.screens.ai.AiAssistantScreen
import com.example.theglobalcarbonfootprintproject.ui.screens.community.LeaderboardScreen

sealed class MainTab(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : MainTab("dashboard_tab", "Dashboard", Icons.Default.Dashboard)
    object History : MainTab("history_tab", "History", Icons.Default.History)
    object AI : MainTab("ai_tab", "Assistant", Icons.Default.AutoAwesome)
    object Community : MainTab("community_tab", "Community", Icons.Default.Leaderboard)
}

@Composable
fun MainContainer(
    onNavigateToLog: () -> Unit
) {
    val navController = rememberNavController()
    val tabs = listOf(MainTab.Dashboard, MainTab.History, MainTab.AI, MainTab.Community)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                tabs.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = MainTab.Dashboard.route, Modifier.padding(innerPadding)) {
            composable(MainTab.Dashboard.route) { 
                DashboardScreen(onNavigateToLog = onNavigateToLog) 
            }
            composable(MainTab.History.route) { HistoryScreen() }
            composable(MainTab.AI.route) { AiAssistantScreen() }
            composable(MainTab.Community.route) { LeaderboardScreen() }
        }
    }
}
