package com.donetimeline.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Date

/** What the phone currently knows about the user's tasks. */
data class TaskSnapshot(
    val tasks: List<Task>,
    /** Local changes not yet confirmed by the server. */
    val pendingWrites: Boolean,
    /** Data came from the on-device cache (usually means offline). */
    val fromCache: Boolean,
)

/**
 * Tasks live in Firestore, which keeps a copy on the phone: writes apply locally at once,
 * work offline, and are uploaded automatically when the network is back.
 */
class TaskRepository(
    private val firestore: FirebaseFirestore,
    private val onWriteError: (Exception) -> Unit,
) {
    private fun tasks(uid: String) = firestore.collection("users").document(uid).collection("tasks")

    fun observe(uid: String): Flow<TaskSnapshot> = callbackFlow {
        val registration = tasks(uid).addSnapshotListener(MetadataChanges.INCLUDE) { snap, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snap != null) {
                trySend(
                    TaskSnapshot(
                        tasks = snap.documents.mapNotNull(::toTask),
                        pendingWrites = snap.metadata.hasPendingWrites(),
                        fromCache = snap.metadata.isFromCache,
                    )
                )
            }
        }
        awaitClose { registration.remove() }
    }

    fun add(uid: String, title: String, notes: String, category: String, completedAt: Long) {
        tasks(uid).document().set(
            fields(title, notes, category, completedAt) + mapOf(
                "starred" to false,
                "createdAt" to FieldValue.serverTimestamp(),
            )
        ).addOnFailureListener(onWriteError)
    }

    fun edit(uid: String, id: String, title: String, notes: String, category: String, completedAt: Long) {
        tasks(uid).document(id).update(fields(title, notes, category, completedAt)).addOnFailureListener(onWriteError)
    }

    fun setStarred(uid: String, id: String, starred: Boolean) {
        tasks(uid).document(id)
            .update(mapOf("starred" to starred, "updatedAt" to FieldValue.serverTimestamp()))
            .addOnFailureListener(onWriteError)
    }

    fun delete(uid: String, id: String) {
        tasks(uid).document(id).delete().addOnFailureListener(onWriteError)
    }

    private fun fields(title: String, notes: String, category: String, completedAt: Long) = mapOf(
        "title" to title.trim().take(200),
        "notes" to notes.trim().take(2000),
        "category" to category.trim().take(40),
        "completedAt" to Timestamp(Date(completedAt)),
        "updatedAt" to FieldValue.serverTimestamp(),
    )

    private fun toTask(doc: DocumentSnapshot): Task? {
        val title = doc.getString("title") ?: return null
        val completed = doc.getTimestamp("completedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return null
        return Task(
            id = doc.id,
            title = title,
            notes = doc.getString("notes").orEmpty(),
            category = doc.getString("category").orEmpty(),
            starred = doc.getBoolean("starred") == true,
            completedAt = completed.toDate().time,
        )
    }
}
