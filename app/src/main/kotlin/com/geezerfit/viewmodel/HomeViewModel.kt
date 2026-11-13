package com.geezerfit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geezerfit.data.repository.PlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val plans: PlanRepository,
) : ViewModel() {

    private val _announcement = MutableStateFlow<String?>(null)
    val announcement: StateFlow<String?> = _announcement.asStateFlow()

    init {
        // Background sync at startup; UI never waits on it.
        viewModelScope.launch {
            val result = plans.sync()
            if (result.updated.isNotEmpty()) {
                _announcement.value = "New plan loaded: " +
                    result.updated.joinToString { "${it.user.replaceFirstChar(Char::uppercase)} — ${it.monthLabel}" }
            }
        }
    }

    fun announcementShown() { _announcement.value = null }
}
