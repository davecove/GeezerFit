package com.softoolsmith.geezerfit.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softoolsmith.geezerfit.data.model.Plan
import com.softoolsmith.geezerfit.data.repository.PlanRepository
import com.softoolsmith.geezerfit.data.repository.remote.USER_IDS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PlanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repo: PlanRepository,
) : ViewModel() {
    val user: String = checkNotNull(savedStateHandle["user"])
    private val userId = USER_IDS[user] ?: 1

    /** null while loading; newest month first. */
    val plans: StateFlow<List<Plan>?> = repo.observePlans(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selected = MutableStateFlow<String?>(null)
    val selected: StateFlow<String?> = _selected.asStateFlow()

    fun select(monthKey: String) { _selected.value = monthKey }
}
