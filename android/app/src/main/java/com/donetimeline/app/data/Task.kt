package com.donetimeline.app.data

/** A completed task, as shown in the UI. Stored in Firestore at users/{uid}/tasks/{id}. */
data class Task(
    val id: String,
    val title: String,
    val notes: String,
    val category: String,
    val starred: Boolean,
    /** Epoch millis. */
    val completedAt: Long,
)
