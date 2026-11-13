package com.geezerfit.data.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext ctx: Context) {
    private val prefs = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _metric = MutableStateFlow(prefs.getBoolean(KEY_METRIC, false))
    /** false = Imperial (lbs + feet), true = Metric (kg + meters). */
    val metric: StateFlow<Boolean> = _metric.asStateFlow()

    fun setMetric(value: Boolean) {
        prefs.edit().putBoolean(KEY_METRIC, value).apply()
        _metric.value = value
    }

    private companion object { const val KEY_METRIC = "metric" }
}
