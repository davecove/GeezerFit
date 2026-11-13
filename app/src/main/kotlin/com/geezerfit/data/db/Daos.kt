package com.geezerfit.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
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
}

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
}

@Dao
interface DraftDao {
    @Query("SELECT * FROM session_draft WHERE userId = :userId AND monthKey = :monthKey AND workoutId = :workoutId")
    suspend fun get(userId: Int, monthKey: String, workoutId: String): SessionDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: SessionDraftEntity)

    @Delete suspend fun delete(draft: SessionDraftEntity)
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
