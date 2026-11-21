package com.donetimeline.app.data

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class AiKind(val key: String) { WEEKLY("weekly"), STANDUP("standup") }

/** What gets sent to the AI: one line per task, a description of the period and (standup) the "done" heading. */
data class AiInput(
    val lines: List<String>,
    val period: String,
    val count: Int,
    val heading: String = "",
    /** The tasks behind [lines], newest first (shown as a preview). */
    val tasks: List<Task> = emptyList(),
)

/** Time window for a standup. Standups only use tasks tagged "Work". */
enum class StandupPeriod(val label: String) {
    SINCE_LAST_WORKDAY("Since yesterday"),
    TODAY("Today"),
    LAST_3_DAYS("Last 3 days"),
    THIS_WEEK("This week");

    /** Chip label; on Mondays "Since yesterday" really means since Friday. */
    fun chipLabel(today: LocalDate = LocalDate.now()) =
        if (this == SINCE_LAST_WORKDAY && today.dayOfWeek in setOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY)) "Since Friday" else label

    fun start(today: LocalDate = LocalDate.now()): LocalDate = when (this) {
        SINCE_LAST_WORKDAY -> today.minusDays(when (today.dayOfWeek) { DayOfWeek.MONDAY -> 3L; DayOfWeek.SUNDAY -> 2L; else -> 1L })
        TODAY -> today
        LAST_3_DAYS -> today.minusDays(2)
        THIS_WEEK -> today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    /** Heading for the "done" section of the standup. */
    fun heading(today: LocalDate = LocalDate.now()) = when (this) {
        SINCE_LAST_WORKDAY -> if (chipLabel(today) == "Since Friday") "Since Friday" else "Yesterday"
        TODAY -> "Done today"
        LAST_3_DAYS -> "Last 3 days"
        THIS_WEEK -> "This week"
    }

    fun description(today: LocalDate = LocalDate.now()) = when (this) {
        SINCE_LAST_WORKDAY -> if (chipLabel(today) == "Since Friday") "since Friday" else "yesterday and today"
        TODAY -> "today"
        LAST_3_DAYS -> "the last 3 days"
        THIS_WEEK -> "this week (since Monday)"
    }
}

fun Task.isWork() = category.trim().equals("work", ignoreCase = true)

/**
 * Calls the Ticked AI proxy (ai-worker/, a Cloudflare Worker that holds the Groq key).
 * The request is authorised with the user's Firebase ID token.
 */
class AiClient(private val auth: FirebaseAuth) {

    suspend fun ask(kind: AiKind, input: AiInput, plan: String = "", blockers: String = ""): String {
        val token = auth.currentUser?.getIdToken(false)?.await()?.token ?: throw AiException("Please sign in again.")
        val body = JSONObject()
            .put("kind", kind.key)
            .put("period", input.period)
            .put("lines", JSONArray(input.lines))
            .put("plan", plan)
            .put("blockers", blockers)
            .put("heading", input.heading)
            .toString()

        return withContext(Dispatchers.IO) {
            val conn = (URL("$AI_URL/v1/ai").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 60_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $token")
            }
            try {
                conn.outputStream.use { it.write(body.toByteArray()) }
                val code = conn.responseCode
                val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(text) }.getOrNull()
                if (code !in 200..299) throw AiException(json?.optString("error")?.ifBlank { null } ?: "Ticked AI failed ($code).")
                json?.optString("text")?.ifBlank { null } ?: throw AiException("Ticked AI returned an empty answer.")
            } catch (e: IOException) {
                throw AiException("Couldn't reach Ticked AI. Check your connection and try again.")
            } finally {
                conn.disconnect()
            }
        }
    }

    companion object {
        /** Deployed Worker URL (see ai-worker/). */
        const val AI_URL = "https://ticked-ai.ticked-ai.workers.dev"

        private val lineFormat = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.ENGLISH)

        /** "Mon 17 Nov, 09:30 · Title [Work] ★ — notes", oldest first. */
        fun lines(tasks: List<Task>, zone: ZoneId = ZoneId.systemDefault()) = tasks.sortedBy { it.completedAt }.map { t ->
            buildString {
                append(Instant.ofEpochMilli(t.completedAt).atZone(zone).format(lineFormat)).append(" · ").append(t.title)
                if (t.category.isNotBlank()) append(" [").append(t.category).append(']')
                if (t.starred) append(" ★")
                if (t.notes.isNotBlank()) append(" — ").append(t.notes.replace(Regex("\\s+"), " ").take(160))
            }
        }

        fun weekly(tasks: List<Task>, zone: ZoneId = ZoneId.systemDefault()): AiInput {
            val start = LocalDate.now(zone).minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
            val list = tasks.filter { it.completedAt >= start }
            return AiInput(lines(list, zone), "the last 7 days", list.size, tasks = list.sortedByDescending { it.completedAt })
        }

        /** Work-tagged tasks in the chosen [period]. */
        fun standup(tasks: List<Task>, period: StandupPeriod, zone: ZoneId = ZoneId.systemDefault()): AiInput {
            val today = LocalDate.now(zone)
            val start = period.start(today).atStartOfDay(zone).toInstant().toEpochMilli()
            val list = tasks.filter { it.completedAt >= start && it.isWork() }
            return AiInput(
                lines = lines(list, zone),
                period = period.description(today),
                count = list.size,
                heading = period.heading(today),
                tasks = list.sortedByDescending { it.completedAt },
            )
        }
    }
}

class AiException(message: String) : Exception(message)
