package com.geezerfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.geezerfit.ui.home.HomeScreen
import com.geezerfit.ui.plan.PlanScreen
import com.geezerfit.ui.session.SessionScreen
import com.geezerfit.ui.theme.GeezerFitTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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
            GeezerFitTheme { HomeScreen(onUserSelected = { nav.navigate("plan/$it") }) }
        }
        composable("plan/{user}", arguments = listOf(navArgument("user") { type = str })) { entry ->
            val user = entry.arguments?.getString("user") ?: "dave"
            GeezerFitTheme(user) {
                PlanScreen(onStartWorkout = { month, workout -> nav.navigate("session/$user/$month/$workout") })
            }
        }
        composable(
            "session/{user}/{month}/{workout}",
            arguments = listOf(
                navArgument("user") { type = str },
                navArgument("month") { type = str },
                navArgument("workout") { type = str },
            ),
        ) { entry ->
            val user = entry.arguments?.getString("user") ?: "dave"
            GeezerFitTheme(user) { SessionScreen(onBackToPlan = { nav.popBackStack() }) }
        }
    }
}
