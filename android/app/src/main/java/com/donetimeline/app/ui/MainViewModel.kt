package com.donetimeline.app.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.donetimeline.app.AppContainer
import com.donetimeline.app.DemoData
import com.donetimeline.app.data.AiClient
import com.donetimeline.app.data.AiInput
import com.donetimeline.app.data.AiKind
import com.donetimeline.app.data.Goal
import com.donetimeline.app.data.GoalPeriod
import com.donetimeline.app.data.GoalProgress
import com.donetimeline.app.data.Task
import com.donetimeline.app.data.progress
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
    ALL("All", null), TODAY("Today", 1), WEEK("7 days", 7), MONTH("30 days", 30)
}

sealed interface Filter {
    data object All : Filter
    data object Starred : Filter
    data class Category(val name: String) : Filter
}

data class Stats(val today: Int, val week: Int, val total: Int, val streak: Int, val bestDay: Int = 0, val starred: Int = 0)

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
    val rangeCounts: Map<Range, Int> = emptyMap(),
    val filter: Filter = Filter.All,
    val range: Range = Range.WEEK,
    val query: String = "",
    val account: Account? = null,
    /** Local changes not yet confirmed by the server. */
    val pendingWrites: Boolean = false,
    /** Showing the on-device copy only (usually offline). */
    val offline: Boolean = false,
    val goals: List<GoalProgress> = emptyList(),
    /** Every task (unfiltered), used for AI summaries. */
    val allTasks: List<Task> = emptyList(),
)

private data class Controls(val filter: Filter, val range: Range, val query: String)

/** Signed-in user plus their tasks; snapshot is null while the first load is running. */
private data class Session(val user: Account?, val snapshot: TaskSnapshot?, val goals: List<Goal> = emptyList())

private fun FirebaseUser.toAccount() = Account(displayName.orEmpty(), email.orEmpty())

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.repository
    private val auth = container.auth
    private val controls = MutableStateFlow(Controls(Filter.All, Range.WEEK, ""))

    private val messages = Channel<String>(Channel.BUFFERED)
    val messageFlow = messages.receiveAsFlow()

    /** Debug-only preview data; when set, it replaces the signed-in session. */
    private val demo = MutableStateFlow<List<Task>?>(null)

    private val firebaseSession = auth.user
        .onStart { emit(auth.currentUser) }
        .flatMapLatest { user ->
            if (user == null) flowOf(Session(null, null))
            else combine(
                repo.observe(user.uid),
                repo.observeGoals(user.uid).onStart { emit(emptyList()) }.catch { emit(emptyList()) },
            ) { snapshot, goals -> Session(user.toAccount(), snapshot, goals) }
                .onStart { emit(Session(user.toAccount(), null)) }
                .catch { e -> onListenError(e) }
        }

    private val demoGoals = MutableStateFlow<List<Goal>>(emptyList())

    private val session = combine(demo, demoGoals, firebaseSession) { d, g, s ->
        if (d != null) Session(Account("Demo", "demo@example.com"), TaskSnapshot(d, pendingWrites = false, fromCache = false), g) else s
    }

    fun startDemo(tasks: List<Task>) {
        demo.value = tasks
        demoGoals.value = DemoData.goals().orEmpty()
    }

    // ---- Goals ----

    fun saveGoal(id: String?, category: String, target: Int, period: GoalPeriod) {
        if (demo.value != null) {
            val goal = Goal(id ?: "g${System.nanoTime()}", category.trim(), target, period)
            demoGoals.value = if (id == null) demoGoals.value + goal else demoGoals.value.map { if (it.id == id) goal else it }
            return
        }
        val uid = uid ?: return
        repo.saveGoal(uid, id, category, target, period)
    }

    fun deleteGoal(id: String) {
        if (demo.value != null) { demoGoals.value = demoGoals.value.filterNot { it.id == id }; return }
        val uid = uid ?: return
        repo.deleteGoal(uid, id)
    }

    // ---- Ticked AI ----

    var standupPeriod by mutableStateOf(com.donetimeline.app.data.StandupPeriod.SINCE_LAST_WORKDAY)

    fun aiInput(kind: AiKind): AiInput = state.value.allTasks.let {
        if (kind == AiKind.WEEKLY) AiClient.weekly(it) else AiClient.standup(it, standupPeriod)
    }

    // Kept here (not in the screen) so results and in-flight requests survive switching tabs.
    val aiResults = mutableStateMapOf<AiKind, String>()
    val aiErrors = mutableStateMapOf<AiKind, String>()
    val aiBusy = mutableStateMapOf<AiKind, Boolean>()
    var standupPlan by mutableStateOf("")
    var standupBlockers by mutableStateOf("")

    fun generateAi(kind: AiKind) {
        if (aiBusy[kind] == true) return
        aiBusy[kind] = true
        aiErrors.remove(kind)
        viewModelScope.launch {
            try {
                aiResults[kind] = askAi(kind, standupPlan, standupBlockers)
            } catch (e: Exception) {
                aiErrors[kind] = e.message ?: "Ticked AI failed."
            } finally {
                aiBusy[kind] = false
            }
        }
    }

    /** Clears a result so the inputs show again (used for "New standup"). */
    fun resetAi(kind: AiKind) {
        aiResults.remove(kind)
        aiErrors.remove(kind)
    }

    /** Returns the AI's text; throws [com.donetimeline.app.data.AiException] with a user-facing message. */
    private suspend fun askAi(kind: AiKind, plan: String = "", blockers: String = ""): String {
        if (demo.value != null) DemoData.ai(kind)?.let { kotlinx.coroutines.delay(900); return it.replace("*Yesterday*", "*${aiInput(kind).heading}*") }
        return container.ai.ask(kind, aiInput(kind), plan, blockers)
    }

    val state: StateFlow<UiState> = combine(session, controls) { s, c -> buildState(s, c) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildState(Session(auth.currentUser?.toAccount(), null), controls.value))

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

    /** Permanently deletes the account and all its tasks. Returns an error message, or null. */
    suspend fun deleteAccount(activityContext: Context): String? =
        auth.deleteAccount(activityContext) { uid -> repo.deleteAll(uid) }

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
        fun matchesFilter(t: Task) = when (val f = c.filter) {
            Filter.All -> true
            Filter.Starred -> t.starred
            is Filter.Category -> t.category == f.name
        }
        val visible = inScope.filter(::matchesFilter)

        // Tab counts: search + category filter applied, for every range.
        val base = all.filter { matchesFilter(it) && (q.isEmpty() || "${it.title} ${it.notes} ${it.category}".lowercase().contains(q)) }
        val rangeCounts = Range.entries.associateWith { r ->
            val s = r.days?.let { today.minusDays((it - 1).toLong()) }
            base.count { s == null || !dateOf(it).isBefore(s) }
        }
        val perDay = all.groupingBy(::dateOf).eachCount()

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
                bestDay = perDay.values.maxOrNull() ?: 0,
                starred = all.count { it.starred },
            ),
            rangeCounts = rangeCounts,

            filter = c.filter,
            range = c.range,
            query = c.query,
            account = session.user,
            pendingWrites = session.snapshot?.pendingWrites == true,
            goals = session.goals.map { it.progress(all, zone) },
            allTasks = all,
            offline = session.snapshot?.fromCache == true,
        )
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { MainViewModel(container) }
        }
    }
}
