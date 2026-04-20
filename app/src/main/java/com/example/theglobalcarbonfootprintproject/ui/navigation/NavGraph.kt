package com.example.theglobalcarbonfootprintproject.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.theglobalcarbonfootprintproject.ui.main.MainContainer
import com.example.theglobalcarbonfootprintproject.ui.screens.log.LogActivityScreen
import com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.OnboardingScreen
import com.example.theglobalcarbonfootprintproject.ui.screens.splash.SplashScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Main : Screen("main")
    object LogActivity : Screen("log_activity")
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(onSplashFinished = {
                navController.navigate(Screen.Onboarding.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onOnboardingComplete = {
                navController.navigate(Screen.Main.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Main.route) {
            MainContainer(onNavigateToLog = {
                navController.navigate(Screen.LogActivity.route)
            })
        }
        composable(Screen.LogActivity.route) {
            LogActivityScreen(onBack = {
                navController.popBackStack()
            })
        }
    }
}
