package com.donetimeline.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.donetimeline.app.AppContainer
import com.donetimeline.app.data.AuthException
import com.donetimeline.app.data.ServerException
import com.donetimeline.app.data.Settings
import com.donetimeline.app.data.SyncOutcome
import com.donetimeline.app.data.TaskEntity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class Range(val label: String, val days: Int?) {
    TODAY("Today", 1), WEEK("7 Days", 7), MONTH("30 Days", 30), ALL("All", null)
}

sealed interface Filter {
    data object All : Filter
    data object Starred : Filter
    data class Category(val name: String) : Filter
}

data class Stats(val today: Int, val week: Int, val total: Int, val streak: Int)

data class DayGroup(val date: LocalDate, val tasks: List<TaskEntity>)

data class UiState(
    val loaded: Boolean = false,
    val totalTasks: Int = 0,
    val groups: List<DayGroup> = emptyList(),
    val categories: List<String> = emptyList(),
    val categoryCounts: Map<String, Int> = emptyMap(),
    val inScopeCount: Int = 0,
    val starredCount: Int = 0,
    val stats: Stats = Stats(0, 0, 0, 0),
    val filter: Filter = Filter.All,
    val range: Range = Range.WEEK,
    val query: String = "",
    val settings: Settings = Settings(),
    val pending: Int = 0,
    val syncing: Boolean = false,
)

private data class Controls(val filter: Filter, val range: Range, val query: String, val syncing: Boolean)

class MainViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.repository
    private val controls = MutableStateFlow(Controls(Filter.All, Range.WEEK, "", false))

    private val messages = Channel<String>(Channel.BUFFERED)
    val messageFlow = messages.receiveAsFlow()

    val state: StateFlow<UiState> = combine(
        repo.tasks, repo.pendingCount, container.settings.settings, controls,
    ) { tasks, pending, settings, c -> buildState(tasks, pending, settings, c) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun setFilter(f: Filter) = controls.update { it.copy(filter = f) }
    fun setRange(r: Range) = controls.update { it.copy(range = r) }
    fun setQuery(q: String) = controls.update { it.copy(query = q) }

    fun save(id: String?, title: String, notes: String, category: String, completedAt: Long) = viewModelScope.launch {
        if (id == null) repo.add(title, notes, category, completedAt)
        else repo.edit(id, title, notes, category, completedAt)
    }

    fun toggleStar(id: String) = viewModelScope.launch { repo.toggleStar(id) }

    fun delete(id: String) = viewModelScope.launch {
        repo.delete(id)
        messages.send("Task deleted")
    }

    fun syncNow(silent: Boolean = false) = viewModelScope.launch {
        if (controls.value.syncing) return@launch
        controls.update { it.copy(syncing = true) }
        try {
            when (val r = repo.sync()) {
                SyncOutcome.NotConfigured -> if (!silent) messages.send("Connect a server in Settings to sync.")
                is SyncOutcome.Success -> if (!silent) messages.send("Synced")
            }
        } catch (e: Exception) {
            if (!silent || e is AuthException) messages.send(describe(e))
        } finally {
            controls.update { it.copy(syncing = false) }
        }
    }

    /** Validates the connection, saves it and runs a first sync. Returns an error message or null. */
    suspend fun connect(url: String, token: String): String? {
        val cleanUrl = url.trim().trimEnd('/')
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            return "The server URL must start with https:// (or http:// for local testing)."
        }
        return try {
            container.api.checkAuth(cleanUrl, token.trim())
            val previous = container.settings.current()
            if (previous.serverUrl.isNotBlank() && previous.serverUrl != cleanUrl) repo.resetForNewServer()
            container.settings.saveConnection(cleanUrl, token)
            syncNow(silent = true)
            null
        } catch (e: Exception) {
            describe(e)
        }
    }

    private fun describe(e: Exception): String = when (e) {
        is AuthException -> "The server rejected the access token. Check it in Settings."
        is IOException -> "Couldn't reach the server. Your changes are saved and will sync later."
        is ServerException -> e.message ?: "Server error"
        else -> e.message ?: "Something went wrong"
    }

    private fun buildState(all: List<TaskEntity>, pending: Int, settings: Settings, c: Controls): UiState {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        fun dateOf(t: TaskEntity) = Instant.ofEpochMilli(t.completedAt).atZone(zone).toLocalDate()

        val start = c.range.days?.let { today.minusDays((it - 1).toLong()) }
        val q = c.query.trim().lowercase()
        val inScope = all.filter { t ->
            (start == null || !dateOf(t).isBefore(start)) &&
                (q.isEmpty() || "${t.title} ${t.notes} ${t.category}".lowercase().contains(q))
        }
        val visible = when (val f = c.filter) {
            Filter.All -> inScope
            Filter.Starred -> inScope.filter { it.starred }
            is Filter.Category -> inScope.filter { it.category == f.name }
        }

        val days = all.map(::dateOf).toSet()
        var cursor = if (today in days) today else today.minusDays(1)
        var streak = 0
        while (cursor in days) { streak++; cursor = cursor.minusDays(1) }
        val weekStart = today.minusDays(6)

        return UiState(
            loaded = true,
            totalTasks = all.size,
            groups = visible.groupBy(::dateOf).map { (d, ts) -> DayGroup(d, ts) },
            categories = all.map { it.category }.filter { it.isNotBlank() }.distinct().sorted(),
            categoryCounts = inScope.filter { it.category.isNotBlank() }.groupingBy { it.category }.eachCount(),
            inScopeCount = inScope.size,
            starredCount = inScope.count { it.starred },
            stats = Stats(
                today = all.count { dateOf(it) == today },
                week = all.count { !dateOf(it).isBefore(weekStart) },
                total = all.size,
                streak = streak,
            ),
            filter = c.filter,
            range = c.range,
            query = c.query,
            settings = settings,
            pending = pending,
            syncing = c.syncing,
        )
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { MainViewModel(container) }
        }
    }
}
