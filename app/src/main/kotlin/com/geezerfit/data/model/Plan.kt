package com.geezerfit.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Plan(
    val user: String,
    @SerialName("month_label") val monthLabel: String,
    @SerialName("month_key") val monthKey: String,
    @SerialName("phase_label") val phaseLabel: String,
    @SerialName("phase_note") val phaseNote: String = "",
    val workouts: List<Workout>,
)

@Serializable
data class Workout(
    val id: String,
    val day: String,
    val warmup: List<WarmupItem> = emptyList(),
    val exercises: List<Exercise>,
    @SerialName("cardio_finisher") val cardioFinisher: CardioFinisher? = null,
)

@Serializable
data class WarmupItem(val name: String, val instruction: String = "")

@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val sets: Int,
    val reps: String,
    @SerialName("rest_sec") val restSec: Int = 0,
    @SerialName("weight_entry") val weightEntry: Boolean = true,
    val notes: String = "",
)

@Serializable
data class CardioFinisher(
    val label: String,
    @SerialName("duration_min") val durationMin: Int,
    val instruction: String = "",
)

@Serializable
data class Manifest(
    val version: Int,
    val updated: String = "",
    @SerialName("base_url") val baseUrl: String,
    val plans: List<ManifestEntry>,
)

@Serializable
data class ManifestEntry(
    val file: String,
    val version: String,
    val user: String,
    @SerialName("month_key") val monthKey: String,
    @SerialName("month_label") val monthLabel: String,
    @SerialName("phase_label") val phaseLabel: String = "",
)
