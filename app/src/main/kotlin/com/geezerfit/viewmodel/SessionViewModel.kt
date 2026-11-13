package com.geezerfit.viewmodel

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geezerfit.data.db.DraftDao
import com.geezerfit.data.db.ExerciseLogEntity
import com.geezerfit.data.db.SessionDao
import com.geezerfit.data.db.SessionDraftEntity
import com.geezerfit.data.db.SessionEntity
import com.geezerfit.data.model.Exercise
import com.geezerfit.data.model.Plan
import com.geezerfit.data.model.Workout
import com.geezerfit.data.repository.PlanRepository
import com.geezerfit.data.repository.remote.USER_IDS
import com.geezerfit.data.settings.SettingsRepository
import com.geezerfit.util.displayToLbs
import com.geezerfit.util.formatNumber
import com.geezerfit.util.lbsToDisplay
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject

@Serializable
data class DraftState(
    val checked: Set<String> = emptySet(),
    val weights: Map<String, String> = emptyMap(),
    val warmup: Set<Int> = emptySet(),
    val cardio: Boolean = false,
)

data class TimerState(val exerciseName: String, val remaining: Int)
data class SavedSummary(val date: String, val workoutLabel: String, val completed: Int)

data class SessionUiState(
    val loading: Boolean = true,
    val plan: Plan? = null,
    val workout: Workout? = null,
    val warmupChecked: Set<Int> = emptySet(),
    val checked: Set<String> = emptySet(),
    val weights: Map<String, String> = emptyMap(),
    val cardioDone: Boolean = false,
    val timer: TimerState? = null,
    val saved: SavedSummary? = null,
    val metric: Boolean = false,
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: PlanRepository,
    private val sessionDao: SessionDao,
    private val draftDao: DraftDao,
    private val json: Json,
    private val settings: SettingsRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val user: String = checkNotNull(savedStateHandle["user"])
    private val month: String = checkNotNull(savedStateHandle["month"])
    private val workoutId: String = checkNotNull(savedStateHandle["workout"])
    private val userId = USER_IDS[user] ?: 1

    private val _state = MutableStateFlow(SessionUiState())
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            val plan = repo.observePlans(userId).first().firstOrNull { it.monthKey == month }
            val workout = plan?.workouts?.firstOrNull { it.id == workoutId }
            if (plan == null || workout == null) {
                _state.update { it.copy(loading = false) }
                return@launch
            }
            val draft = draftDao.get(userId, month, workoutId)
                ?.let { runCatching { json.decodeFromString<DraftState>(it.stateJson) }.getOrNull() }
            val weights = workout.exercises.filter { it.weightEntry }.associate { ex ->
                ex.id to (draft?.weights?.get(ex.id)
                    ?: sessionDao.lastWeight(userId, ex.id)?.let { formatNumber(lbsToDisplay(it, settings.metric.value)) }
                    ?: "")
            }
            _state.update {
                it.copy(
                    loading = false, plan = plan, workout = workout, weights = weights, metric = settings.metric.value,
                    checked = draft?.checked ?: emptySet(),
                    warmupChecked = draft?.warmup ?: emptySet(),
                    cardioDone = draft?.cardio ?: false,
                )
            }
        }
    }

    fun toggleWarmup(index: Int) {
        _state.update { s ->
            s.copy(warmupChecked = if (index in s.warmupChecked) s.warmupChecked - index else s.warmupChecked + index)
        }
        persist()
    }

    fun toggleCardio() {
        _state.update { it.copy(cardioDone = !it.cardioDone) }
        persist()
    }

    fun toggleExercise(ex: Exercise) {
        val nowChecked = ex.id !in _state.value.checked
        _state.update { s -> s.copy(checked = if (nowChecked) s.checked + ex.id else s.checked - ex.id) }
        if (nowChecked && ex.restSec > 0) startTimer(ex) else if (!nowChecked) cancelTimerFor(ex)
        persist()
    }

    fun setWeight(exerciseId: String, text: String) {
        val clean = text.filter { it.isDigit() || it == '.' }.take(6)
        _state.update { it.copy(weights = it.weights + (exerciseId to clean)) }
        persist()
    }

    fun skipTimer() {
        timerJob?.cancel()
        _state.update { it.copy(timer = null) }
    }

    private fun cancelTimerFor(ex: Exercise) {
        if (_state.value.timer?.exerciseName == ex.name) skipTimer()
    }

    private fun startTimer(ex: Exercise) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for (r in ex.restSec downTo 1) {
                _state.update { it.copy(timer = TimerState(ex.name, r)) }
                delay(1000)
            }
            _state.update { it.copy(timer = null) }
            vibrate()
        }
    }

    private fun vibrate() {
        runCatching {
            context.getSystemService(Vibrator::class.java)
                ?.vibrate(VibrationEffect.createOneShot(600, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    /** Auto-save so an interrupted workout can be resumed. */
    private fun persist() {
        val s = _state.value
        viewModelScope.launch {
            draftDao.upsert(
                SessionDraftEntity(
                    userId, month, workoutId, planVersion = month,
                    stateJson = json.encodeToString(DraftState(s.checked, s.weights, s.warmupChecked, s.cardioDone)),
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    fun finish() {
        val s = _state.value
        val workout = s.workout ?: return
        timerJob?.cancel()
        viewModelScope.launch {
            val done = workout.exercises.filter { it.id in s.checked }
            val today = LocalDate.now().toString()
            sessionDao.saveSession(
                SessionEntity(userId = userId, date = today, monthKey = month, workoutId = workoutId,
                    completedAt = System.currentTimeMillis()),
                done.map {
                    ExerciseLogEntity(
                        sessionId = 0, exerciseId = it.id, exerciseName = it.name, setsCompleted = it.sets,
                        weightLbs = if (it.weightEntry) s.weights[it.id]?.toFloatOrNull()?.let { w -> displayToLbs(w, s.metric) } else null,
                    )
                },
            )
            draftDao.clear(userId, month, workoutId)
            _state.update {
                it.copy(timer = null, saved = SavedSummary(today, "Workout ${workout.id} — ${workout.day}", done.size))
            }
        }
    }
}
