package com.geezerfit.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geezerfit.data.db.ExerciseLogEntity
import com.geezerfit.data.db.SessionDao
import com.geezerfit.data.db.SessionEntity
import com.geezerfit.data.repository.remote.USER_IDS
import com.geezerfit.data.settings.SettingsRepository
import com.geezerfit.util.displayToLbs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dao: SessionDao,
    private val settings: SettingsRepository,
) : ViewModel() {
    val user: String = checkNotNull(savedStateHandle["user"])
    private val userId = USER_IDS[user] ?: 1

    val metric: StateFlow<Boolean> = settings.metric

    val sessions: StateFlow<List<SessionEntity>> = dao.observeHistory(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _expanded = MutableStateFlow<Long?>(null)
    val expanded: StateFlow<Long?> = _expanded.asStateFlow()

    private val _logs = MutableStateFlow<Map<Long, List<ExerciseLogEntity>>>(emptyMap())
    val logs: StateFlow<Map<Long, List<ExerciseLogEntity>>> = _logs.asStateFlow()

    fun toggle(sessionId: Long) {
        if (_expanded.value == sessionId) { _expanded.value = null; return }
        _expanded.value = sessionId
        viewModelScope.launch { reload(sessionId) }
    }

    private suspend fun reload(sessionId: Long) {
        val l = dao.logsFor(sessionId)
        _logs.update { it + (sessionId to l) }
    }

    /** [text] is in the currently selected display unit; blank clears the weight. */
    fun updateWeight(log: ExerciseLogEntity, text: String) {
        val lbs = text.toFloatOrNull()?.let { displayToLbs(it, metric.value) }
        viewModelScope.launch {
            dao.setLogWeight(log.id, lbs)
            dao.refreshLastWeight(userId, log.exerciseId)
            reload(log.sessionId)
        }
    }

    fun removeLog(log: ExerciseLogEntity) {
        viewModelScope.launch {
            dao.deleteLog(log.id)
            dao.refreshLastWeight(userId, log.exerciseId)
            reload(log.sessionId)
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            val affected = dao.logsFor(sessionId).map { it.exerciseId }.distinct()
            dao.deleteSession(sessionId)
            affected.forEach { dao.refreshLastWeight(userId, it) }
            _logs.update { it - sessionId }
            if (_expanded.value == sessionId) _expanded.value = null
        }
    }
}
