package com.example.theglobalcarbonfootprintproject.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.theglobalcarbonfootprintproject.ui.main.MainContainer
import com.example.theglobalcarbonfootprintproject.ui.screens.log.LogActivityScreen
import com.example.theglobalcarbonfootprintproject.ui.screens.onboarding.OnboardingScreen
import com.example.theglobalcarbonfootprintproject.ui.screens.splash.SplashScreen
import com.example.theglobalcarbonfootprintproject.ui.screens.profile.ProfileScreen
import com.example.theglobalcarbonfootprintproject.ui.screens.metrics.MetricsScreen
import androidx.compose.ui.platform.LocalContext
import android.content.Context

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Main : Screen("main")
    object LogActivity : Screen("log_activity")
    object Profile : Screen("profile")
    object Metrics : Screen("metrics")
}

@Composable
fun NavGraph(deepLinkUri: Uri? = null) {
    val navController = rememberNavController()
    val context = LocalContext.current

    LaunchedEffect(deepLinkUri) {
        deepLinkUri?.let { uri ->
            when (uri.path) {
                "/food/breakfast", "/food", "/energy", "/log" -> {
                    navController.navigate(Screen.LogActivity.route)
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(onSplashFinished = {
                val prefs = context.getSharedPreferences("carbon_prefs", Context.MODE_PRIVATE)
                val onboardingComplete = prefs.getBoolean("onboarding_complete", false)
                
                if (onboardingComplete) {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                } else {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
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
            MainContainer(
                onNavigateToLog = {
                    navController.navigate(Screen.LogActivity.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }
        composable(Screen.LogActivity.route) {
            LogActivityScreen(onBack = {
                navController.popBackStack()
            })
        }
        composable(Screen.Profile.route) {
            // ProfileScreen implemented below
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onNavigateToMetrics = { navController.navigate(Screen.Metrics.route) },
                onSignOut = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Metrics.route) {
            MetricsScreen(onBack = { navController.popBackStack() })
        }
    }
}

// ProfileScreen and MetricsScreen will be created in separate files or added here if they are small.
// Better to create separate files as per instructions.
