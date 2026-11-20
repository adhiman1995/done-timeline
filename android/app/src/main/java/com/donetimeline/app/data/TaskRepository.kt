package com.donetimeline.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

sealed interface SyncOutcome {
    data object NotConfigured : SyncOutcome
    data class Success(val pushed: Int, val pulled: Int) : SyncOutcome
}

/**
 * Offline-first repository. Every edit is written to Room immediately (marked dirty)
 * and [onLocalChange] asks the scheduler to push it when a network is available.
 *
 * Sync protocol (see server POST /api/sync):
 *   1. Send all dirty rows plus our cursor.
 *   2. Server applies them last-write-wins on updatedAt and returns every row with rev > cursor.
 *   3. Mark the pushed rows clean (unless edited again meanwhile), then apply the server rows,
 *      skipping any that are older than a still-dirty local edit.
 */
class TaskRepository(
    private val db: AppDatabase,
    private val settings: SettingsStore,
    private val api: ApiClient,
    private val onLocalChange: () -> Unit,
) {
    private val dao = db.tasks()
    private val syncLock = Mutex()

    val tasks: Flow<List<TaskEntity>> = dao.observeVisible()
    val pendingCount: Flow<Int> = dao.observePendingCount()

    suspend fun add(title: String, notes: String, category: String, completedAt: Long) {
        val now = System.currentTimeMillis()
        dao.upsert(
            TaskEntity(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                notes = notes.trim(),
                category = category.trim(),
                completedAt = completedAt,
                createdAt = now,
                updatedAt = now,
                dirty = true,
            )
        )
        onLocalChange()
    }

    suspend fun update(id: String, change: (TaskEntity) -> TaskEntity) {
        val current = dao.get(id) ?: return
        // updatedAt must strictly increase so last-write-wins sees this as the newer version.
        val stamp = maxOf(System.currentTimeMillis(), current.updatedAt + 1)
        dao.upsert(change(current).copy(id = current.id, updatedAt = stamp, dirty = true))
        onLocalChange()
    }

    suspend fun edit(id: String, title: String, notes: String, category: String, completedAt: Long) =
        update(id) { it.copy(title = title.trim(), notes = notes.trim(), category = category.trim(), completedAt = completedAt) }

    suspend fun toggleStar(id: String) = update(id) { it.copy(starred = !it.starred) }

    suspend fun delete(id: String) = update(id) { it.copy(deleted = true) }

    suspend fun sync(): SyncOutcome = syncLock.withLock {
        val s = settings.current()
        if (!s.isConfigured) return SyncOutcome.NotConfigured

        val dirty = dao.dirty()
        val response = api.sync(s.serverUrl, s.token, SyncRequest(since = s.cursor, changes = dirty.map { it.toDto() }))

        db.withTransaction {
            dirty.forEach { dao.markClean(it.id, it.updatedAt) }
            for (remote in response.changes) {
                val incoming = remote.toEntity()
                val local = dao.get(incoming.id)
                if (local != null && local.dirty && local.updatedAt > incoming.updatedAt) continue
                dao.upsert(incoming)
            }
            dao.purgeSyncedDeletes()
        }
        settings.recordSync(response.cursor, System.currentTimeMillis())
        SyncOutcome.Success(pushed = dirty.size, pulled = response.changes.size)
    }

    /** Called when switching servers: local data belongs to the old server. Unsynced edits are kept. */
    suspend fun resetForNewServer() {
        db.withTransaction {
            val pending = dao.dirty()
            dao.clear()
            pending.forEach { dao.upsert(it) }
        }
    }
}
