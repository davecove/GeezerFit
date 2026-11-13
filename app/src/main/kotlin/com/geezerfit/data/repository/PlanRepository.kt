package com.geezerfit.data.repository

import com.geezerfit.data.model.ManifestEntry
import com.geezerfit.data.model.Plan
import kotlinx.coroutines.flow.Flow

data class SyncResult(
    val updated: List<ManifestEntry>,
    val error: String? = null,
)

interface PlanRepository {
    /** Cached plans for a user, newest month first. Emits immediately from cache. */
    fun observePlans(userId: Int): Flow<List<Plan>>

    /** Fetch manifest + any changed plans. Never throws; failures are reported in [SyncResult.error]. */
    suspend fun sync(): SyncResult
}
