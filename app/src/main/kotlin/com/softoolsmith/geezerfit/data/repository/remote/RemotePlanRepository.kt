package com.softoolsmith.geezerfit.data.repository.remote

import com.softoolsmith.geezerfit.data.db.PlanCacheDao
import com.softoolsmith.geezerfit.data.db.PlanCacheEntity
import com.softoolsmith.geezerfit.data.model.Manifest
import com.softoolsmith.geezerfit.data.model.ManifestEntry
import com.softoolsmith.geezerfit.data.model.Plan
import com.softoolsmith.geezerfit.data.repository.PlanRepository
import com.softoolsmith.geezerfit.data.repository.PlanStatus
import com.softoolsmith.geezerfit.data.repository.SyncResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

const val MANIFEST_URL =
    "https://raw.githubusercontent.com/davecove/GeezerFit/main/plans/manifest.json"

val USER_IDS = mapOf("dave" to 1, "lesa" to 2)

@Singleton
class RemotePlanRepository @Inject constructor(
    private val dao: PlanCacheDao,
    private val client: OkHttpClient,
    private val json: Json,
) : PlanRepository {

    override fun observeStatus(): Flow<PlanStatus> =
        dao.observeStatus().map { PlanStatus(it.count, it.lastCachedAt) }

    override fun observePlans(userId: Int): Flow<List<Plan>> =
        dao.observeForUser(userId).map { rows ->
            rows.mapNotNull { runCatching { json.decodeFromString<Plan>(it.jsonContent) }.getOrNull() }
        }

    override suspend fun sync(): SyncResult = withContext(Dispatchers.IO) {
        try {
            val manifest = json.decodeFromString<Manifest>(fetch(MANIFEST_URL))
            val updated = mutableListOf<ManifestEntry>()
            for (entry in manifest.plans) {
                val userId = USER_IDS[entry.user] ?: continue
                val cached = dao.get(userId, entry.monthKey)
                if (cached?.version == entry.version) continue // any difference triggers a download
                val body = runCatching { fetch(manifest.baseUrl + entry.file) }.getOrNull() ?: continue
                // Validate: must parse and match the manifest entry; otherwise keep the cached copy.
                val plan = runCatching { json.decodeFromString<Plan>(body) }.getOrNull() ?: continue
                if (plan.monthKey != entry.monthKey || plan.user != entry.user) continue
                dao.upsert(PlanCacheEntity(userId, entry.monthKey, entry.version, body, System.currentTimeMillis()))
                updated += entry
            }
            SyncResult(updated)
        } catch (e: Exception) {
            SyncResult(emptyList(), e.message ?: "Sync failed")
        }
    }

    private fun fetch(url: String): String {
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            check(resp.isSuccessful) { "HTTP ${resp.code} for $url" }
            return resp.body!!.string()
        }
    }
}
