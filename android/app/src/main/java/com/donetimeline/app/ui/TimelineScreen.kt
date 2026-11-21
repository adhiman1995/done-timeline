package com.donetimeline.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.donetimeline.app.data.Task
import com.donetimeline.app.ui.theme.Done
import com.donetimeline.app.ui.theme.categoryColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val TimeCol = 60.dp
private val RailCol = 26.dp
private val NodeY = 33.dp // card top padding (14) + half the category icon (38 / 2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(vm: MainViewModel, onOpenSettings: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var editor by remember { mutableStateOf<EditorTarget?>(null) }
    val c = Done.colors

    LaunchedEffect(Unit) { vm.messageFlow.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        containerColor = c.bg,
        topBar = { TopBar(state, vm, onOpenSettings) },
        snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = 72.dp)) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
                ) {
                    item("head") { PageHeader(state) }
                    item("stats") { StatsRow(state.stats) }
                    item("range") {
                        Tabs(
                            options = Range.entries, selected = state.range, label = { it.label },
                            count = { state.rangeCounts[it] }, onSelect = vm::setRange,
                            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                        )
                    }
                    item("filters") { Box(Modifier.padding(top = 12.dp)) { FilterRow(state, vm) } }
                    item("list-head") { ListHeader(state) }

                    if (state.loaded && state.groups.isEmpty()) {
                        item("empty") { EmptyState(state, vm) }
                    }

                    state.groups.forEachIndexed { gi, group ->
                        item(key = "day-${group.date}") { DayHeader(group, first = gi == 0) }
                        group.tasks.forEachIndexed { ti, task ->
                            item(key = task.id) {
                                TimelineEntry(
                                    task = task,
                                    first = ti == 0,
                                    last = ti == group.tasks.lastIndex,
                                    onClick = { editor = EditorTarget(task) },
                                    onStar = { vm.toggleStar(task.id) },
                                )
                            }
                        }
                    }
                }
            }

            // Bottom dock with the primary action.
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, c.bg.copy(alpha = 0.95f), c.bg)))
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                GradientButton(
                    text = "Add completed task",
                    onClick = { editor = EditorTarget(null) },
                    height = 52.dp,
                    modifier = Modifier.fillMaxWidth(),
                    leading = {
                        Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                    },
                )
            }
        }
    }

    editor?.let { target ->
        TaskEditorSheet(
            task = target.task,
            categories = state.categories,
            onDismiss = { editor = null },
            onSave = { title, notes, category, completedAt ->
                vm.save(target.task?.id, title, notes, category, completedAt)
                editor = null
            },
            onDelete = target.task?.let { t -> { vm.delete(t.id); editor = null } },
        )
    }
}

private data class EditorTarget(val task: Task?)

/* ---------------------------------------------------------------- Top bar */

@Composable
private fun TopBar(state: UiState, vm: MainViewModel, onOpenSettings: () -> Unit) {
    val c = Done.colors
    var searching by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.background(c.bg).statusBarsPadding()) {
        AnimatedContent(searching, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "topbar") { isSearching ->
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(start = 16.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isSearching) {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = vm::setQuery,
                        placeholder = { Text("Search tasks") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = c.muted) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors(),
                        modifier = Modifier.weight(1f).height(52.dp),
                    )
                    IconButton(onClick = { vm.setQuery(""); searching = false }) {
                        Icon(Icons.Filled.Close, "Close search", tint = c.text2)
                    }
                } else {
                    AppLogo()
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Ticked", color = c.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        SyncStatus(state)
                    }
                    IconButton(onClick = { searching = true }) { Icon(Icons.Filled.Search, "Search", tint = c.text2) }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.AccountCircle, "Account", tint = c.text2) }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
    }
}

@Composable
private fun SyncStatus(state: UiState) {
    val c = Done.colors
    when {
        !state.loaded -> StatusLine("Loading…")
        state.offline && state.pendingWrites -> StatusLine("Offline · changes saved on this phone", c.star)
        state.pendingWrites -> StatusLine("Syncing…", c.accentText)
        state.offline -> StatusLine("Offline · showing saved tasks")
        else -> StatusLine("Synced · up to date")
    }
}

/* ---------------------------------------------------------------- Header content */

@Composable
private fun PageHeader(state: UiState) {
    val c = Done.colors
    val now = remember { java.time.LocalDateTime.now() }
    val shown = state.groups.sumOf { it.tasks.size }
    val rangeText = when (state.range) {
        Range.ALL -> "all time"; Range.TODAY -> "today"; Range.WEEK -> "last 7 days"; Range.MONTH -> "last 30 days"
    }
    Column(Modifier.padding(top = 8.dp, bottom = 16.dp)) {
        Text("Timeline", color = c.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
        Text(
            "${now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))} · $shown task${if (shown == 1) "" else "s"}, $rangeText",
            color = c.text2, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun StatsRow(stats: Stats) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard("All time", "${stats.total}", Modifier.weight(1f))
        StatCard("Streak", "${stats.streak}", Modifier.weight(1f), suffix = if (stats.streak == 1) "day" else "days", flame = stats.streak > 0)
        StatCard("Best day", "${stats.bestDay}", Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier, suffix: String? = null, flame: Boolean = false) {
    val c = Done.colors
    DoneCard(modifier) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(label, color = c.text2, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp)) {
                Text(value, color = c.text, fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp, letterSpacing = (-0.5).sp)
                if (suffix != null) {
                    Spacer(Modifier.width(4.dp))
                    Text(suffix, color = c.muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                }
                if (flame) Text(" 🔥", fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

@Composable
private fun ListHeader(state: UiState) {
    val c = Done.colors
    val n = state.groups.sumOf { it.tasks.size }
    Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$n task${if (n == 1) "" else "s"}", color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Text("Newest first", color = c.muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Box(Modifier.clip(CircleShape).background(c.accentSoft).padding(horizontal = 10.dp, vertical = 4.dp)) {
            Text(state.range.label, color = c.accentText, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FilterRow(state: UiState, vm: MainViewModel) {
    val dark = Done.colors.isDark
    // Bleed to the screen edges so pills scroll under the margins instead of being clipped.
    LazyRow(
        modifier = Modifier.fillMaxWidth().bleed(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { FilterPill("All", state.inScopeCount, state.filter == Filter.All, { vm.setFilter(Filter.All) }) }
        items(state.categories) { cat ->
            val selected = state.filter == Filter.Category(cat)
            FilterPill(
                label = cat,
                count = state.categoryCounts[cat] ?: 0,
                selected = selected,
                onClick = { vm.setFilter(if (selected) Filter.All else Filter.Category(cat)) },
                dotColor = categoryColors(cat, dark).dot,
            )
        }
        item {
            val selected = state.filter == Filter.Starred
            FilterPill("Starred", state.starredCount, selected, { vm.setFilter(if (selected) Filter.All else Filter.Starred) }, starred = true)
        }
    }
}

@Composable
private fun EmptyState(state: UiState, vm: MainViewModel) {
    val c = Done.colors
    val (title, body, action) = when {
        state.totalTasks == 0 -> Triple("Your timeline starts here", "Log the first thing you finished and it will show up here.", null)
        state.query.isNotBlank() || state.filter != Filter.All ->
            Triple("No matching tasks", "Try a different search, category or range.", "Clear filters")
        else -> Triple("Nothing in this period", "No tasks were completed in the selected range.", "Show all time")
    }
    DoneCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 40.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(c.hover),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Add, null, tint = c.accentText) }
            Spacer(Modifier.height(14.dp))
            Text(title, color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Spacer(Modifier.height(4.dp))
            Text(body, color = c.text2, fontSize = 14.sp, textAlign = TextAlign.Center)
            if (action != null) {
                TextButton(onClick = {
                    if (state.query.isBlank() && state.filter == Filter.All) vm.setRange(Range.ALL)
                    vm.setQuery(""); vm.setFilter(Filter.All)
                }) { Text(action, color = c.accentText) }
            }
        }
    }
}

/* ---------------------------------------------------------------- Timeline rows */

private val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

private fun dayLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern(if (date.year == today.year) "EEE, MMM d" else "EEE, MMM d, yyyy"))
    }
}

/** Draws the rail segment for one row plus a filled node at [nodeY]. */
private fun Modifier.rail(
    lineColor: Color, nodeColor: Color, ringColor: Color,
    nodeY: androidx.compose.ui.unit.Dp, lineFromNode: Boolean, lineToNode: Boolean,
) = drawBehind {
    val x = size.width / 2
    val y = nodeY.toPx()
    drawLine(lineColor, Offset(x, if (lineFromNode) y else 0f), Offset(x, if (lineToNode) y else size.height), 2.dp.toPx())
    drawCircle(ringColor, radius = 10.dp.toPx(), center = Offset(x, y))
    drawCircle(nodeColor, radius = 6.dp.toPx(), center = Offset(x, y))
}

private fun dayTone(date: LocalDate, c: com.donetimeline.app.ui.theme.DoneColors) = when {
    date == LocalDate.now() -> c.today
    !date.isBefore(LocalDate.now().minusDays(6)) -> c.week
    else -> c.older
}

@Composable
private fun DayHeader(group: DayGroup, first: Boolean) {
    val c = Done.colors
    Row(
        Modifier.padding(top = if (first) 0.dp else 20.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToneBadge(dayLabel(group.date), dayTone(group.date, c))
        Spacer(Modifier.width(10.dp))
        Text("${group.tasks.size} completed", color = c.muted, fontSize = 12.5.sp)
    }
}

@Composable
private fun TimelineEntry(task: Task, first: Boolean, last: Boolean, onClick: () -> Unit, onStar: () -> Unit) {
    val c = Done.colors
    val time = remember(task.completedAt) {
        Instant.ofEpochMilli(task.completedAt).atZone(ZoneId.systemDefault()).format(timeFormat)
    }
    val nodeColor = categoryColors(task.category, c.isDark).dot

    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Text(
            time,
            color = c.text2, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, maxLines = 1,
            modifier = Modifier.width(TimeCol).padding(top = NodeY - 9.dp),
        )
        Box(
            Modifier.width(RailCol).fillMaxHeight()
                .rail(c.rail, nodeColor, c.bg, nodeY = NodeY, lineFromNode = first, lineToNode = last),
        )
        DoneCard(Modifier.weight(1f).padding(start = 4.dp, bottom = 8.dp), onClick = onClick) {
            Row(Modifier.padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 4.dp)) {
                CategoryIcon(task.category)
                Column(Modifier.weight(1f).padding(start = 12.dp, top = 1.dp)) {
                    Text(task.title, color = c.text, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
                    if (task.notes.isNotBlank()) {
                        Text(task.notes, color = c.text2, fontSize = 13.5.sp, lineHeight = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                    }
                    if (task.category.isNotBlank()) {
                        CategoryChip(task.category, Modifier.padding(top = 8.dp))
                    }
                }
                IconButton(onClick = onStar, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = if (task.starred) "Unstar" else "Star",
                        tint = if (task.starred) c.star else c.muted.copy(alpha = 0.25f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
