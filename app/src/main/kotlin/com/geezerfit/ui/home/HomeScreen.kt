package com.geezerfit.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.geezerfit.viewmodel.HomeViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeScreen(onUserSelected: (String) -> Unit, onSettings: () -> Unit, vm: HomeViewModel = hiltViewModel()) {
    val announcement by vm.announcement.collectAsState()
    val status by vm.status.collectAsState()
    val syncError by vm.syncError.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(announcement) {
        announcement?.let {
            snackbar.showSnackbar(it)
            vm.announcementShown()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("GeezerFit", style = MaterialTheme.typography.displaySmall)
            UserButton("Dave", Color(0xFF1F4E79)) { onUserSelected("dave") }
            UserButton("Lesa", Color(0xFF375623)) { onUserSelected("lesa") }

            val updated = status.lastUpdated?.let {
                " · updated " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it))
            } ?: ""
            TextButton(onClick = onSettings) { Text("Settings", style = MaterialTheme.typography.titleMedium) }
            Text("Plans: ${status.count} loaded$updated", style = MaterialTheme.typography.bodyMedium)
            syncError?.let {
                Text("Couldn't check for new plans: $it", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun UserButton(name: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(72.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
    ) { Text(name, style = MaterialTheme.typography.headlineSmall) }
}
