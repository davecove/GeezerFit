package com.softoolsmith.geezerfit.viewmodel

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softoolsmith.geezerfit.data.db.SessionDao
import com.softoolsmith.geezerfit.data.repository.PlanRepository
import com.softoolsmith.geezerfit.data.repository.PlanStatus
import com.softoolsmith.geezerfit.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject

enum class ExportFormat(val extension: String, val mime: String) {
    CSV("csv", "text/csv"),
    JSON("json", "application/json"),
}

@Serializable
private data class ExportLog(
    val exerciseId: String, val exerciseName: String, val setsCompleted: Int, val weightLbs: Float?,
)

@Serializable
private data class ExportSession(
    val user: String, val date: String, val monthKey: String, val workoutId: String,
    val completedAt: Long, val exercises: List<ExportLog>,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val plans: PlanRepository,
    private val dao: SessionDao,
    private val json: Json,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val metric: StateFlow<Boolean> = settings.metric
    fun setMetric(v: Boolean) = settings.setMetric(v)

    val status: StateFlow<PlanStatus> = plans.observeStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlanStatus(0, null))

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    fun messageShown() { _message.value = null }

    val versionName: String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "?"

    fun refresh() {
        viewModelScope.launch {
            _message.value = "Checking for new plans…"
            val r = plans.sync()
            _message.value = when {
                r.error != null -> "Couldn't check for new plans: ${r.error}"
                r.updated.isEmpty() -> "Plans are up to date."
                else -> "Updated: " + r.updated.joinToString { "${it.user} ${it.monthLabel}" }
            }
        }
    }

    /** Writes an export file to the app cache and returns a shareable content Uri. */
    suspend fun buildExport(format: ExportFormat): Uri = withContext(Dispatchers.IO) {
        val sessions = dao.allSessions()
        val logsBySession = dao.allLogs().groupBy { it.sessionId }
        val userName = { id: Int -> if (id == 1) "dave" else "lesa" }

        val text = when (format) {
            ExportFormat.CSV -> buildString {
                appendLine("user,date,month_key,workout_id,exercise_id,exercise_name,sets_completed,weight_lbs")
                for (s in sessions) for (l in logsBySession[s.id].orEmpty()) {
                    appendLine(
                        listOf(userName(s.userId), s.date, s.monthKey, s.workoutId, l.exerciseId,
                            "\"" + l.exerciseName.replace("\"", "\"\"") + "\"", l.setsCompleted, l.weightLbs ?: "")
                            .joinToString(",")
                    )
                }
            }
            ExportFormat.JSON -> json.encodeToString(
                sessions.map { s ->
                    ExportSession(userName(s.userId), s.date, s.monthKey, s.workoutId, s.completedAt,
                        logsBySession[s.id].orEmpty().map { ExportLog(it.exerciseId, it.exerciseName, it.setsCompleted, it.weightLbs) })
                }
            )
        }

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "geezerfit_history.${format.extension}")
        file.writeText(text)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
