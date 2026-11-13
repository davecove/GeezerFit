package com.softoolsmith.geezerfit.ui.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.softoolsmith.geezerfit.viewmodel.PlanViewModel
import java.time.YearMonth

@Composable
fun PlanScreen(onStartWorkout: (month: String, workout: String) -> Unit, onHistory: () -> Unit, vm: PlanViewModel = hiltViewModel()) {
    val plans by vm.plans.collectAsState()
    val selectedKey by vm.selected.collectAsState()

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(vm.user.replaceFirstChar(Char::uppercase), style = MaterialTheme.typography.headlineMedium)

            OutlinedButton(onClick = onHistory, Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("History") }

            val list = plans
            when {
                list == null -> Text("Loading…")
                list.isEmpty() -> Text("No plans loaded yet. Connect to the internet and reopen the app.")
                else -> {
                    val current = YearMonth.now().toString()
                    val effective = selectedKey?.takeIf { k -> list.any { it.monthKey == k } }
                        ?: current.takeIf { c -> list.any { it.monthKey == c } }
                        ?: list.first().monthKey
                    val plan = list.first { it.monthKey == effective }

                    if (list.none { it.monthKey == current }) {
                        Text(
                            "New plan not yet available for this month. Using ${plan.monthLabel}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        list.forEach { p ->
                            FilterChip(
                                selected = p.monthKey == effective,
                                onClick = { vm.select(p.monthKey) },
                                label = { Text(p.monthLabel, style = MaterialTheme.typography.bodyLarge) },
                            )
                        }
                    }

                    Text("${plan.phaseLabel}", style = MaterialTheme.typography.titleMedium)
                    if (plan.phaseNote.isNotBlank()) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Text(plan.phaseNote, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    plan.workouts.forEach { w ->
                        Card(
                            Modifier.fillMaxWidth().clickable { onStartWorkout(plan.monthKey, w.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        ) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Workout ${w.id} — ${w.day}", style = MaterialTheme.typography.titleLarge)
                                Text("${w.exercises.size} exercises", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}
