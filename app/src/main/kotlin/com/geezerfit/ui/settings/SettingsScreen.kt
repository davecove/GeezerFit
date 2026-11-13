package com.geezerfit.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.geezerfit.viewmodel.ExportFormat
import com.geezerfit.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val metric by vm.metric.collectAsState()
    val status by vm.status.collectAsState()
    val message by vm.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); vm.messageShown() }
    }

    fun export(format: ExportFormat) {
        scope.launch {
            runCatching {
                val uri = vm.buildExport(format)
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = format.mime
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(send, "Export history"))
            }.onFailure { snackbar.showSnackbar("Export failed: ${it.message}") }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)

            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Units", style = MaterialTheme.typography.titleMedium)
                    Text(if (metric) "Metric (kg, meters)" else "Imperial (lbs, feet)", style = MaterialTheme.typography.bodyMedium)
                }
                Switch(checked = metric, onCheckedChange = vm::setMetric)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Plans", style = MaterialTheme.typography.titleMedium)
                val updated = status.lastUpdated?.let {
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it))
                } ?: "never"
                Text("${status.count} loaded · last updated $updated", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = vm::refresh, Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Check for new plans") }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Export history", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { export(ExportFormat.CSV) }, Modifier.weight(1f).heightIn(min = 56.dp)) { Text("CSV") }
                    OutlinedButton(onClick = { export(ExportFormat.JSON) }, Modifier.weight(1f).heightIn(min = 56.dp)) { Text("JSON") }
                }
            }

            Text("Version ${vm.versionName}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
