package com.geezerfit.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.geezerfit.data.db.ExerciseLogEntity
import com.geezerfit.util.formatNumber
import com.geezerfit.util.formatWeight
import com.geezerfit.util.lbsToDisplay
import com.geezerfit.util.unitLabel
import com.geezerfit.viewmodel.HistoryViewModel

@Composable
fun HistoryScreen(vm: HistoryViewModel = hiltViewModel()) {
    val sessions by vm.sessions.collectAsState()
    val expanded by vm.expanded.collectAsState()
    val logs by vm.logs.collectAsState()
    val metric by vm.metric.collectAsState()

    var editing by remember { mutableStateOf<ExerciseLogEntity?>(null) }
    var deletingSession by remember { mutableStateOf<Long?>(null) }

    Scaffold { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text("${vm.user.replaceFirstChar(Char::uppercase)} — History", style = MaterialTheme.typography.headlineMedium) }
            if (sessions.isEmpty()) item { Text("No sessions yet.", style = MaterialTheme.typography.bodyLarge) }
            items(sessions, key = { it.id }) { s ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.fillMaxWidth().clickable { vm.toggle(s.id) }) {
                            Text(s.date, style = MaterialTheme.typography.titleMedium)
                            Text("Workout ${s.workoutId} · ${s.monthKey}", style = MaterialTheme.typography.bodyLarge)
                        }
                        if (expanded == s.id) {
                            val rows = logs[s.id].orEmpty()
                            if (rows.isEmpty()) Text("No exercises recorded.")
                            rows.forEach { l ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { editing = l },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(l.exerciseName, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                    Text(l.weightLbs?.let { formatWeight(it, metric) } ?: "—", fontSize = 16.sp)
                                }
                            }
                            TextButton(onClick = { deletingSession = s.id }) { Text("Delete session") }
                        }
                    }
                }
            }
        }
    }

    editing?.let { log ->
        var text by remember(log.id) {
            mutableStateOf(log.weightLbs?.let { formatNumber(lbsToDisplay(it, metric)) } ?: "")
        }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(log.exerciseName) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                    label = { Text("Weight (${unitLabel(metric)})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            },
            confirmButton = { TextButton(onClick = { vm.updateWeight(log, text); editing = null }) { Text("Save") } },
            dismissButton = {
                Row {
                    TextButton(onClick = { vm.removeLog(log); editing = null }) { Text("Remove exercise") }
                    TextButton(onClick = { editing = null }) { Text("Cancel") }
                }
            },
        )
    }

    deletingSession?.let { id ->
        AlertDialog(
            onDismissRequest = { deletingSession = null },
            title = { Text("Delete this session?") },
            text = { Text("This can't be undone.") },
            confirmButton = { TextButton(onClick = { vm.deleteSession(id); deletingSession = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deletingSession = null }) { Text("Cancel") } },
        )
    }
}
