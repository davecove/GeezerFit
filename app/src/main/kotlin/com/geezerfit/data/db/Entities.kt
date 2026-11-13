package com.geezerfit.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** user_id: 1 = Dave, 2 = Lesa */
@Entity(tableName = "session")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Int,
    val date: String, // ISO 8601
    val monthKey: String,
    val workoutId: String,
    val completedAt: Long,
)

@Entity(
    tableName = "exercise_log",
    foreignKeys = [ForeignKey(SessionEntity::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionId")],
)
data class ExerciseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: String,
    val exerciseName: String, // display snapshot
    val setsCompleted: Int,
    val weightLbs: Float?,
)

@Entity(tableName = "last_weight", primaryKeys = ["userId", "exerciseId"])
data class LastWeightEntity(
    val userId: Int,
    val exerciseId: String,
    val weightLbs: Float,
)

@Entity(tableName = "plan_cache", primaryKeys = ["userId", "monthKey"])
data class PlanCacheEntity(
    val userId: Int,
    val monthKey: String,
    val version: String,
    val jsonContent: String,
    val cachedAt: Long,
)

/** In-progress session, auto-saved so an interrupted workout can resume. */
@Entity(tableName = "session_draft", primaryKeys = ["userId", "monthKey", "workoutId"])
data class SessionDraftEntity(
    val userId: Int,
    val monthKey: String,
    val workoutId: String,
    val planVersion: String,
    val stateJson: String, // checked exercise ids + weights
    val updatedAt: Long,
)
