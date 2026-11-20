package com.donetimeline.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit

@Serializable
data class TaskDto(
    val id: String,
    val title: String,
    val notes: String = "",
    val category: String = "",
    val starred: Boolean = false,
    val completedAt: String,
    val createdAt: String? = null,
    val updatedAt: String,
    val deleted: Boolean = false,
)

@Serializable
data class SyncRequest(val since: Long, val changes: List<TaskDto>)

@Serializable
data class SyncResponse(val cursor: Long, val changes: List<TaskDto>)

fun TaskEntity.toDto() = TaskDto(
    id = id,
    title = title,
    notes = notes,
    category = category,
    starred = starred,
    completedAt = Instant.ofEpochMilli(completedAt).toString(),
    createdAt = Instant.ofEpochMilli(createdAt).toString(),
    updatedAt = Instant.ofEpochMilli(updatedAt).toString(),
    deleted = deleted,
)

fun TaskDto.toEntity(): TaskEntity {
    val updated = Instant.parse(updatedAt).toEpochMilli()
    return TaskEntity(
        id = id,
        title = title,
        notes = notes,
        category = category,
        starred = starred,
        completedAt = Instant.parse(completedAt).toEpochMilli(),
        createdAt = createdAt?.let { Instant.parse(it).toEpochMilli() } ?: updated,
        updatedAt = updated,
        deleted = deleted,
        dirty = false,
    )
}

/** The server rejected the access token. Retrying won't help until the user fixes it. */
class AuthException : Exception("The server rejected the access token.")

/** The server answered with an error other than 401. */
class ServerException(code: Int, message: String) : Exception("Server error $code: $message")

class ApiClient {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val jsonType = "application/json".toMediaType()

    // Generous read timeout: free hosting tiers can take ~60s to wake up.
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    suspend fun checkAuth(serverUrl: String, token: String) {
        execute(Request.Builder().url("${base(serverUrl)}/api/auth/check").header("Authorization", "Bearer $token").get().build())
    }

    suspend fun sync(serverUrl: String, token: String, request: SyncRequest): SyncResponse {
        val body = json.encodeToString(SyncRequest.serializer(), request).toRequestBody(jsonType)
        val req = Request.Builder()
            .url("${base(serverUrl)}/api/sync")
            .header("Authorization", "Bearer $token")
            .post(body)
            .build()
        return json.decodeFromString(SyncResponse.serializer(), execute(req))
    }

    private fun base(url: String) = url.trim().trimEnd('/')

    private suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        val call = try {
            http.newCall(request)
        } catch (e: IllegalArgumentException) {
            throw IOException("Invalid server URL")
        }
        call.execute().use { res ->
            val text = res.body?.string().orEmpty()
            when {
                res.code == 401 -> throw AuthException()
                !res.isSuccessful -> throw ServerException(res.code, text.take(200))
                else -> text
            }
        }
    }
}
