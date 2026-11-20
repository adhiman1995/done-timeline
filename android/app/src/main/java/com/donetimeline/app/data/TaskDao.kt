package com.donetimeline.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE deleted = 0 ORDER BY completedAt DESC")
    fun observeVisible(): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE dirty = 1")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE dirty = 1")
    suspend fun dirty(): List<TaskEntity>

    @Upsert
    suspend fun upsert(task: TaskEntity)

    /** Clears the dirty flag only if the row wasn't edited again while the sync was in flight. */
    @Query("UPDATE tasks SET dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markClean(id: String, updatedAt: Long)

    /** Removes tombstones the server already knows about. */
    @Query("DELETE FROM tasks WHERE deleted = 1 AND dirty = 0")
    suspend fun purgeSyncedDeletes()

    @Query("DELETE FROM tasks")
    suspend fun clear()
}
