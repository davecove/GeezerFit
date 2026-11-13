package com.geezerfit.ui.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.geezerfit.data.model.Exercise
import com.geezerfit.util.localizeDistance
import com.geezerfit.util.unitLabel
import com.geezerfit.viewmodel.SessionViewModel

@Composable
fun SessionScreen(onBackToPlan: () -> Unit, onViewHistory: () -> Unit, vm: SessionViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    var confirming by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current

    val plan = s.plan
    val workout = s.workout

    when {
        s.loading -> { Text("Loading…", Modifier.padding(24.dp)); return }
        plan == null || workout == null -> {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("That workout isn't available.")
                Button(onClick = onBackToPlan) { Text("Back") }
            }
            return
        }
        s.saved != null -> {
            val saved = s.saved!!
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Session saved. Good work.", style = MaterialTheme.typography.headlineSmall)
                Text(saved.date, style = MaterialTheme.typography.bodyLarge)
                Text(saved.workoutLabel, style = MaterialTheme.typography.bodyLarge)
                Text("${saved.completed} ${if (saved.completed == 1) "exercise" else "exercises"} completed", style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onViewHistory, Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("View History") }
                OutlinedButton(onClick = onBackToPlan, Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Back to Plan") }
            }
            return
        }
    }

    Scaffold(
        bottomBar = {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                s.timer?.let { t ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Rest: ${t.remaining}s  (${t.exerciseName})", style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = vm::skipTimer) { Text("Skip") }
                        }
                    }
                }
                Button(
                    onClick = { confirming = true },
                    enabled = s.checked.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) { Text("Finish Workout", fontSize = 18.sp) }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Workout ${workout.id} — ${workout.day}", style = MaterialTheme.typography.headlineSmall)
                    Text("${plan.monthLabel} · ${plan.phaseLabel}", style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (plan.phaseNote.isNotBlank()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Text(plan.phaseNote, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (workout.warmup.isNotEmpty()) {
                item {
                    var open by rememberSaveable { mutableStateOf(true) }
                    Column {
                        Text(
                            (if (open) "▼ " else "▶ ") + "Warm-Up",
                            Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = 12.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (open) {
                            workout.warmup.forEachIndexed { i, w ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { vm.toggleWarmup(i) }.heightIn(min = 48.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(checked = i in s.warmupChecked, onCheckedChange = { vm.toggleWarmup(i) })
                                    Column {
                                        Text(w.name, style = MaterialTheme.typography.bodyLarge)
                                        if (w.instruction.isNotBlank())
                                            Text(w.instruction, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Text("Exercises", style = MaterialTheme.typography.titleMedium) }
            items(workout.exercises, key = { it.id }) { ex ->
                ExerciseCard(
                    ex = ex,
                    metric = s.metric,
                    checked = ex.id in s.checked,
                    weight = s.weights[ex.id] ?: "",
                    onToggle = { vm.toggleExercise(ex) },
                    onWeight = { vm.setWeight(ex.id, it) },
                    onDone = { focus.clearFocus() },
                )
            }

            workout.cardioFinisher?.let { c ->
                item {
                    Card {
                        Row(
                            Modifier.fillMaxWidth().clickable { vm.toggleCardio() }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = s.cardioDone, onCheckedChange = { vm.toggleCardio() })
                            Column {
                                Text("${c.label} — ${c.durationMin} min", fontSize = 18.sp)
                                Text(c.instruction, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            item { Text("", Modifier.padding(8.dp)) }
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Save this session?") },
            confirmButton = { TextButton(onClick = { confirming = false; vm.finish() }) { Text("Yes") } },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ExerciseCard(
    ex: Exercise,
    metric: Boolean,
    checked: Boolean,
    weight: String,
    onToggle: () -> Unit,
    onWeight: (String) -> Unit,
    onDone: () -> Unit,
) {
    var notesOpen by rememberSaveable(ex.id) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().alpha(if (checked) 0.55f else 1f)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth().clickable { onToggle() }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = checked, onCheckedChange = { onToggle() })
                Column(Modifier.weight(1f)) {
                    Text(ex.name, fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${ex.sets} × ${localizeDistance(ex.reps, metric)}" + if (ex.restSec > 0) "   Rest: ${ex.restSec}s" else "",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            if (ex.weightEntry) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = onWeight,
                        label = { Text("Weight") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onDone() }),
                        modifier = Modifier.width(160.dp),
                    )
                    Text("  ${unitLabel(metric)}", style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (ex.notes.isNotBlank()) {
                Text(
                    (if (notesOpen) "▼ " else "▶ ") + "Coaching notes",
                    Modifier.fillMaxWidth().clickable { notesOpen = !notesOpen }.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
                if (notesOpen) Text(localizeDistance(ex.notes, metric), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
