package com.geezerfit.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanCacheDao {
    @Query("SELECT * FROM plan_cache WHERE userId = :userId ORDER BY monthKey DESC")
    fun observeForUser(userId: Int): Flow<List<PlanCacheEntity>>

    @Query("SELECT * FROM plan_cache WHERE userId = :userId AND monthKey = :monthKey")
    suspend fun get(userId: Int, monthKey: String): PlanCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PlanCacheEntity)

    @Query("SELECT COUNT(*) AS count, MAX(cachedAt) AS lastCachedAt FROM plan_cache")
    fun observeStatus(): Flow<PlanStatusRow>
}

data class PlanStatusRow(val count: Int, val lastCachedAt: Long?)

@Dao
interface SessionDao {
    @Insert suspend fun insertSession(session: SessionEntity): Long
    @Insert suspend fun insertLogs(logs: List<ExerciseLogEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertLastWeights(weights: List<LastWeightEntity>)

    @Transaction
    suspend fun saveSession(session: SessionEntity, logs: List<ExerciseLogEntity>) {
        val id = insertSession(session)
        insertLogs(logs.map { it.copy(sessionId = id) })
        upsertLastWeights(
            logs.mapNotNull { l ->
                l.weightLbs?.let { LastWeightEntity(session.userId, l.exerciseId, it) }
            }
        )
    }

    @Query("SELECT * FROM session WHERE userId = :userId ORDER BY completedAt DESC")
    fun observeHistory(userId: Int): Flow<List<SessionEntity>>

    @Query("SELECT * FROM exercise_log WHERE sessionId = :sessionId")
    suspend fun logsFor(sessionId: Long): List<ExerciseLogEntity>

    @Query("DELETE FROM session WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("SELECT weightLbs FROM last_weight WHERE userId = :userId AND exerciseId = :exerciseId")
    suspend fun lastWeight(userId: Int, exerciseId: String): Float?

    @Query("UPDATE exercise_log SET weightLbs = :weight WHERE id = :id")
    suspend fun setLogWeight(id: Long, weight: Float?)

    @Query("DELETE FROM exercise_log WHERE id = :id")
    suspend fun deleteLog(id: Long)

    @Query("SELECT l.weightLbs FROM exercise_log l JOIN session s ON s.id = l.sessionId WHERE s.userId = :userId AND l.exerciseId = :exerciseId AND l.weightLbs IS NOT NULL ORDER BY s.completedAt DESC LIMIT 1")
    suspend fun latestLoggedWeight(userId: Int, exerciseId: String): Float?

    @Query("DELETE FROM last_weight WHERE userId = :userId AND exerciseId = :exerciseId")
    suspend fun clearLastWeight(userId: Int, exerciseId: String)

    /** Recompute the pre-fill weight after an edit/delete so it matches the newest remaining log. */
    @Transaction
    suspend fun refreshLastWeight(userId: Int, exerciseId: String) {
        val w = latestLoggedWeight(userId, exerciseId)
        if (w == null) clearLastWeight(userId, exerciseId)
        else upsertLastWeights(listOf(LastWeightEntity(userId, exerciseId, w)))
    }

    @Query("SELECT * FROM session ORDER BY completedAt ASC")
    suspend fun allSessions(): List<SessionEntity>

    @Query("SELECT * FROM exercise_log")
    suspend fun allLogs(): List<ExerciseLogEntity>
}

@Dao
interface DraftDao {
    @Query("SELECT * FROM session_draft WHERE userId = :userId AND monthKey = :monthKey AND workoutId = :workoutId")
    suspend fun get(userId: Int, monthKey: String, workoutId: String): SessionDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: SessionDraftEntity)

    @Query("DELETE FROM session_draft WHERE userId = :userId AND monthKey = :monthKey AND workoutId = :workoutId")
    suspend fun clear(userId: Int, monthKey: String, workoutId: String)
}

@Database(
    entities = [
        SessionEntity::class, ExerciseLogEntity::class, LastWeightEntity::class,
        PlanCacheEntity::class, SessionDraftEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun planCacheDao(): PlanCacheDao
    abstract fun sessionDao(): SessionDao
    abstract fun draftDao(): DraftDao
}
