package com.geezerfit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geezerfit.data.repository.PlanRepository
import com.geezerfit.data.repository.PlanStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val plans: PlanRepository,
) : ViewModel() {

    private val _announcement = MutableStateFlow<String?>(null)
    val announcement: StateFlow<String?> = _announcement.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    val status: StateFlow<PlanStatus> = plans.observeStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlanStatus(0, null))

    init {
        // Background sync at startup; UI never waits on it.
        viewModelScope.launch {
            val result = plans.sync()
            _syncError.value = result.error
            if (result.updated.isNotEmpty()) {
                _announcement.value = "New plan loaded: " +
                    result.updated.joinToString { "${it.user.replaceFirstChar(Char::uppercase)} — ${it.monthLabel}" }
            }
        }
    }

    fun announcementShown() { _announcement.value = null }
}
