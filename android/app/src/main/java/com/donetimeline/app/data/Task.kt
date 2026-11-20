package com.donetimeline.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A completed task, stored locally first.
 *
 * - [dirty] marks rows changed on this device that the server hasn't acknowledged yet.
 * - [deleted] is a tombstone so deletes can be synced; deleted rows are hidden from the UI
 *   and purged once the server has them.
 * - All timestamps are epoch milliseconds (UTC).
 */
@Entity(tableName = "tasks", indices = [Index("dirty"), Index("completedAt")])
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val notes: String = "",
    val category: String = "",
    val starred: Boolean = false,
    val completedAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val dirty: Boolean = false,
)
