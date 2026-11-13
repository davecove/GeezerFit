package com.geezerfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.geezerfit.ui.home.HomeScreen
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
    NavHost(nav, startDestination = "home") {
        composable("home") {
            GeezerFitTheme { HomeScreen(onUserSelected = { nav.navigate("plan/$it") }) }
        }
        composable("plan/{user}", arguments = listOf(navArgument("user") { type = NavType.StringType })) { entry ->
            val user = entry.arguments?.getString("user")
            GeezerFitTheme(user) {
                // TODO: Plan screen (month tabs, workout cards), Session, History, Settings per spec
                Text("Plan screen for $user -- not built yet")
            }
        }
    }
}
