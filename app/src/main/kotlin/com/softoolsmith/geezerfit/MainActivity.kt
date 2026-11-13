package com.softoolsmith.geezerfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.softoolsmith.geezerfit.ui.history.HistoryScreen
import com.softoolsmith.geezerfit.ui.home.HomeScreen
import com.softoolsmith.geezerfit.ui.plan.PlanScreen
import com.softoolsmith.geezerfit.ui.session.SessionScreen
import com.softoolsmith.geezerfit.ui.settings.SettingsScreen
import com.softoolsmith.geezerfit.ui.theme.GeezerFitTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { GeezerFitRoot() }
    }
}

@Composable
private fun GeezerFitRoot() {
    val nav = rememberNavController()
    val str = NavType.StringType
    NavHost(nav, startDestination = "home") {
        composable("home") {
            GeezerFitTheme { HomeScreen(onUserSelected = { nav.navigate("plan/$it") }, onSettings = { nav.navigate("settings") }) }
        }
        composable("plan/{user}", arguments = listOf(navArgument("user") { type = str })) { entry ->
            val user = entry.arguments?.getString("user") ?: "dave"
            GeezerFitTheme(user) {
                PlanScreen(
                    onStartWorkout = { month, workout -> nav.navigate("session/$user/$month/$workout") },
                    onHistory = { nav.navigate("history/$user") },
                )
            }
        }
        composable("history/{user}", arguments = listOf(navArgument("user") { type = str })) { entry ->
            val user = entry.arguments?.getString("user") ?: "dave"
            GeezerFitTheme(user) { HistoryScreen() }
        }
        composable("settings") { GeezerFitTheme { SettingsScreen() } }
        composable(
            "session/{user}/{month}/{workout}",
            arguments = listOf(
                navArgument("user") { type = str },
                navArgument("month") { type = str },
                navArgument("workout") { type = str },
            ),
        ) { entry ->
            val user = entry.arguments?.getString("user") ?: "dave"
            GeezerFitTheme(user) {
                SessionScreen(
                    onBackToPlan = { nav.popBackStack() },
                    onViewHistory = { nav.navigate("history/$user") { popUpTo("plan/$user") } },
                )
            }
        }
    }
}
