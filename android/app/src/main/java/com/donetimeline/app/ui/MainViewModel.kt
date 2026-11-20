package com.donetimeline.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.donetimeline.app.AppContainer
import com.donetimeline.app.data.Task
import com.donetimeline.app.data.TaskSnapshot
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

data class DayGroup(val date: LocalDate, val tasks: List<Task>)

data class Account(val name: String, val email: String)

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
    val account: Account? = null,
    /** Local changes not yet confirmed by the server. */
    val pendingWrites: Boolean = false,
    /** Showing the on-device copy only (usually offline). */
    val offline: Boolean = false,
)

private data class Controls(val filter: Filter, val range: Range, val query: String)

/** Signed-in user plus their tasks; snapshot is null while the first load is running. */
private data class Session(val user: FirebaseUser?, val snapshot: TaskSnapshot?)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.repository
    private val auth = container.auth
    private val controls = MutableStateFlow(Controls(Filter.All, Range.WEEK, ""))

    private val messages = Channel<String>(Channel.BUFFERED)
    val messageFlow = messages.receiveAsFlow()

    private val session = auth.user
        .onStart { emit(auth.currentUser) }
        .flatMapLatest { user ->
            if (user == null) flowOf(Session(null, null))
            else repo.observe(user.uid)
                .map { Session(user, it) }
                .onStart { emit(Session(user, null)) }
                .catch { e -> onListenError(e) }
        }

    val state: StateFlow<UiState> = combine(session, controls) { s, c -> buildState(s, c) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildState(Session(auth.currentUser, null), controls.value))

    init {
        viewModelScope.launch { container.writeErrors.collect { messages.send(describe(it)) } }
    }

    fun setFilter(f: Filter) = controls.update { it.copy(filter = f) }
    fun setRange(r: Range) = controls.update { it.copy(range = r) }
    fun setQuery(q: String) = controls.update { it.copy(query = q) }

    private val uid get() = auth.currentUser?.uid

    fun save(id: String?, title: String, notes: String, category: String, completedAt: Long) {
        val uid = uid ?: return
        if (id == null) repo.add(uid, title, notes, category, completedAt)
        else repo.edit(uid, id, title, notes, category, completedAt)
    }

    fun toggleStar(id: String) {
        val uid = uid ?: return
        val task = state.value.groups.flatMap { it.tasks }.firstOrNull { it.id == id } ?: return
        repo.setStarred(uid, id, !task.starred)
    }

    fun delete(id: String) {
        val uid = uid ?: return
        repo.delete(uid, id)
        viewModelScope.launch { messages.send("Task deleted") }
    }

    /** Returns an error message, or null on success / cancel. */
    suspend fun signIn(activityContext: Context): String? = auth.signIn(activityContext)

    fun signOut(context: Context) = viewModelScope.launch { auth.signOut(context) }

    private suspend fun onListenError(e: Throwable) {
        if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            val email = auth.currentUser?.email.orEmpty()
            messages.send("$email doesn't have access to this timeline.")
            auth.signOut(container.appContext)
        } else {
            messages.send(e.message ?: "Couldn't load tasks.")
        }
    }

    private fun describe(e: Exception): String = when {
        e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "This account isn't allowed to change these tasks."
        else -> e.message ?: "Something went wrong"
    }

    private fun buildState(session: Session, c: Controls): UiState {
        val all = session.snapshot?.tasks.orEmpty()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        fun dateOf(t: Task) = Instant.ofEpochMilli(t.completedAt).atZone(zone).toLocalDate()

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
            loaded = session.snapshot != null,
            totalTasks = all.size,
            groups = visible.sortedByDescending { it.completedAt }.groupBy(::dateOf).map { (d, ts) -> DayGroup(d, ts) },
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
            account = session.user?.let { Account(it.displayName.orEmpty(), it.email.orEmpty()) },
            pendingWrites = session.snapshot?.pendingWrites == true,
            offline = session.snapshot?.fromCache == true,
        )
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { MainViewModel(container) }
        }
    }
}
