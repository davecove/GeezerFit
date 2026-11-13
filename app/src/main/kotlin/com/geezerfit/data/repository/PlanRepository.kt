package com.geezerfit.data.repository

import com.geezerfit.data.model.ManifestEntry
import com.geezerfit.data.model.Plan
import kotlinx.coroutines.flow.Flow

data class SyncResult(
    val updated: List<ManifestEntry>,
    val error: String? = null,
)

data class PlanStatus(val count: Int, val lastUpdated: Long?)

interface PlanRepository {
    /** How many plans are cached and when the cache last changed. */
    fun observeStatus(): Flow<PlanStatus>

    /** Cached plans for a user, newest month first. Emits immediately from cache. */
    fun observePlans(userId: Int): Flow<List<Plan>>

    /** Fetch manifest + any changed plans. Never throws; failures are reported in [SyncResult.error]. */
    suspend fun sync(): SyncResult
}
