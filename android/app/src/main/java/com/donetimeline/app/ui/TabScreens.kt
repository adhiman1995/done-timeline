package com.donetimeline.app.ui

import android.content.ClipData
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import com.donetimeline.app.data.AiInput
import com.donetimeline.app.data.GoalPeriod
import com.donetimeline.app.data.StandupPeriod
import kotlin.math.roundToInt
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.donetimeline.app.R
import com.donetimeline.app.data.AiKind
import com.donetimeline.app.data.Goal
import com.donetimeline.app.data.GoalProgress
import com.donetimeline.app.ui.theme.Done
import com.donetimeline.app.ui.theme.categoryColors
import kotlinx.coroutines.launch

/* ------------------------------------------------------------------ Shell with bottom tabs */

enum class MainTab(val label: String) { HOME("Home"), GOALS("Goals"), AI("AI") }

/** Signed-in app: Home (timeline), Goals and AI, switched with a bottom bar. */
@Composable
fun MainShell(vm: MainViewModel, onOpenSettings: () -> Unit) {
    val c = Done.colors
    var tab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    BackHandler(enabled = tab != MainTab.HOME) { tab = MainTab.HOME }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                MainTab.HOME -> TimelineScreen(vm, onOpenSettings)
                MainTab.GOALS -> GoalsScreen(vm, onOpenSettings)
                MainTab.AI -> AiScreen(vm, onOpenSettings)
            }
        }
        BottomNav(tab) { tab = it }
    }
}

@Composable
private fun BottomNav(selected: MainTab, onSelect: (MainTab) -> Unit) {
    val c = Done.colors
    Column(Modifier.fillMaxWidth().background(c.surface)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(68.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MainTab.entries.forEach { t ->
                val active = t == selected
                val painter: Painter = when (t) {
                    MainTab.HOME -> rememberVectorPainter(Icons.Filled.Home)
                    MainTab.GOALS -> painterResource(R.drawable.ic_target)
                    MainTab.AI -> painterResource(R.drawable.ic_sparkle)
                }
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(role = Role.Tab) { onSelect(t) }
                        .semantics { this.selected = active },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        Modifier.width(60.dp).height(32.dp).clip(CircleShape)
                            .background(if (active) c.accentSoft else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painter, contentDescription = null, tint = if (active) c.accentText else c.muted, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        t.label, fontSize = 12.sp,
                        color = if (active) c.accentText else c.muted,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

/** Header used by the Goals tab (same look as Home's). */
@Composable
private fun TabTopBar(title: String, subtitle: String, onOpenSettings: () -> Unit) {
    val c = Done.colors
    Column(Modifier.background(c.bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(start = 16.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            AppLogo()
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = c.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                StatusLine(subtitle)
            }
            IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.AccountCircle, "Account", tint = c.text2) }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
    }
}

/** Soft white light bloom in the top-right corner of brand-gradient cards. */
private fun Modifier.glow() = drawBehind {
    drawCircle(
        Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
            center = Offset(size.width * 0.85f, size.height * 0.1f), radius = size.width * 0.45f,
        ),
        radius = size.width * 0.45f, center = Offset(size.width * 0.85f, size.height * 0.1f),
    )
}

/* ================================================================== Goals tab */

private data class GoalTarget(val goal: Goal?)

private val quickGoals = listOf(
    Triple("Health", 4, GoalPeriod.WEEK),
    Triple("Work", 3, GoalPeriod.DAY),
    Triple("Learning", 3, GoalPeriod.WEEK),
)

@Composable
fun GoalsScreen(vm: MainViewModel, onOpenSettings: () -> Unit) {
    val c = Done.colors
    val state by vm.state.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<GoalTarget?>(null) }
    val goals = state.goals
    val met = goals.count { it.met }

    Column(Modifier.fillMaxSize().background(c.bg)) {
        TabTopBar("Goals", if (goals.isEmpty()) "Targets that fill up as you log" else "$met of ${goals.size} on track", onOpenSettings)
        Box(Modifier.weight(1f)) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (goals.isEmpty()) {
                    item("empty") {
                        EmptyGoals(
                            onAdd = { editor = GoalTarget(null) },
                            onQuickAdd = { (cat, target, period) -> vm.saveGoal(null, cat, target, period) },
                        )
                    }
                } else {
                    item("summary") { GoalsSummary(goals) }
                    GoalPeriod.entries.forEach { period ->
                        val group = goals.filter { it.goal.period == period }
                        if (group.isNotEmpty()) {
                            item("h-${period.key}") {
                                Row(Modifier.padding(top = 10.dp, start = 4.dp), verticalAlignment = Alignment.Bottom) {
                                    Text(period.now.replaceFirstChar { it.uppercase() }, color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(8.dp))
                                    Text("${group.count { it.met }}/${group.size} done", color = c.muted, fontSize = 13.sp)
                                }
                            }
                            items(group, key = { it.goal.id }) { p -> GoalCard(p) { editor = GoalTarget(p.goal) } }
                        }
                    }
                }
            }
            AddButton(
                onClick = { editor = GoalTarget(null) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 20.dp),
            )
        }
    }

    editor?.let { target ->
        GoalSheet(
            goal = target.goal,
            categories = state.categories,
            onSave = { category, goalTarget, period -> vm.saveGoal(target.goal?.id, category, goalTarget, period); editor = null },
            onDelete = target.goal?.let { g -> { vm.deleteGoal(g.id); editor = null } },
            onDismiss = { editor = null },
        )
    }
}

/** Brand-gradient card with the overall progress ring. */
@Composable
private fun GoalsSummary(goals: List<GoalProgress>) {
    val c = Done.colors
    val met = goals.count { it.met }
    val overall = goals.map { it.ratio }.average().toFloat()
    val remaining = goals.sumOf { (it.goal.target - it.done).coerceAtLeast(0) }
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.logoGradient).glow(),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(92.dp)) {
                    val stroke = 9.dp.toPx()
                    val inset = stroke / 2
                    val arc = Size(size.width - stroke, size.height - stroke)
                    drawArc(Color.White.copy(alpha = 0.25f), 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
                    drawArc(Color.White, -90f, 360f * overall, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Text("${(overall * 100).roundToInt()}%", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(18.dp))
            Column {
                Text("$met of ${goals.size} on track", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (remaining == 0) "Every goal is done. Great work! 🎉"
                    else "$remaining more task${if (remaining == 1) "" else "s"} to hit every goal",
                    color = Color.White.copy(alpha = 0.88f), fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun GoalCard(p: GoalProgress, onClick: () -> Unit) {
    val c = Done.colors
    val accent = if (p.met) GoalGreen else categoryColors(p.goal.category, c.isDark).dot
    DoneCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIcon(p.goal.category, 42.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.goal.name, color = c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("${p.goal.target} ${p.goal.period.per}", color = c.muted, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${p.done}", color = if (p.met) GoalGreen else c.text, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                    Text("/${p.goal.target}", color = c.muted, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 3.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            if (p.goal.target <= 12) {
                // One segment per task in the target.
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(p.goal.target) { i ->
                        Box(
                            Modifier.weight(1f).height(8.dp).clip(CircleShape)
                                .background(if (i < p.done) accent else c.hover),
                        )
                    }
                }
            } else {
                Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(c.hover)) {
                    Box(Modifier.fillMaxWidth(p.ratio.coerceAtLeast(0.02f)).fillMaxHeight().clip(CircleShape).background(accent))
                }
            }
            Spacer(Modifier.height(10.dp))
            if (p.met) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, null, tint = GoalGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Goal met ${p.goal.period.now}", color = GoalGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                val left = p.goal.target - p.done
                Text("$left more to go ${p.goal.period.now}", color = c.text2, fontSize = 13.sp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyGoals(onAdd: () -> Unit, onQuickAdd: (Triple<String, Int, GoalPeriod>) -> Unit) {
    val c = Done.colors
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.logoGradient).glow()) {
        Column(Modifier.padding(22.dp)) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_target), null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Text("Set your first goal", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
            Text(
                "Pick a target and it fills up automatically as you log tasks.",
                color = Color.White.copy(alpha = 0.88f), fontSize = 14.5.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
            )
            Box(
                Modifier.fillMaxWidth().height(48.dp).clip(CircleShape).background(Color.White).clickable(role = Role.Button, onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) { Text("Create a goal", color = Color(0xFF2E6BE6), fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
        }
    }
    Text("Or start with one of these", color = c.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 18.dp, bottom = 10.dp, start = 4.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        quickGoals.forEach { g ->
            val cc = categoryColors(g.first, c.isDark)
            Row(
                Modifier.clip(CircleShape).background(c.surface).border(1.dp, c.border, CircleShape)
                    .clickable(role = Role.Button) { onQuickAdd(g) }.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(cc.dot))
                Spacer(Modifier.width(8.dp))
                Text("${g.first} · ${g.second} ${g.third.per}", color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/* ================================================================== AI tab */

@Composable
fun AiScreen(vm: MainViewModel, onOpenSettings: () -> Unit) {
    val c = Done.colors
    val state by vm.state.collectAsStateWithLifecycle()
    var kind by rememberSaveable { mutableStateOf(AiKind.WEEKLY) }
    val input = remember(kind, state.allTasks, vm.standupPeriod) { vm.aiInput(kind) }
    val text = vm.aiResults[kind]
    val busy = vm.aiBusy[kind] == true
    val error = vm.aiErrors[kind]

    LightSystemBarIcons()

    Column(Modifier.fillMaxSize().background(c.bg)) {
        AiHeader(kind, onKind = { kind = it }, onOpenSettings = onOpenSettings)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                busy -> AiLoading(kind)
                text != null -> AiResult(vm, kind, text, input)
                kind == AiKind.WEEKLY -> WeeklyIntro(input) { vm.generateAi(kind) }
                else -> StandupForm(vm, input) { vm.generateAi(kind) }
            }
            error?.let {
                Text(
                    it, color = c.danger, fontSize = 13.5.sp,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.danger.copy(alpha = 0.08f)).padding(12.dp),
                )
            }
            Text(
                "AI can make mistakes. Your tasks are sent to Groq only to write this text.",
                color = c.muted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
    }
}

/** Brand-gradient header with the Weekly review / Standup switch. */
@Composable
private fun AiHeader(kind: AiKind, onKind: (AiKind) -> Unit, onOpenSettings: () -> Unit) {
    val c = Done.colors
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)).background(c.logoGradient).glow()) {
        Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_sparkle), null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Ticked AI", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Turn finished work into words", color = Color.White.copy(alpha = 0.85f), fontSize = 13.5.sp)
                }
                IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.AccountCircle, "Account", tint = Color.White) }
            }
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.padding(end = 12.dp).fillMaxWidth().height(46.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)).padding(4.dp),
            ) {
                AiKind.entries.forEach { k ->
                    val active = k == kind
                    Box(
                        Modifier.weight(1f).fillMaxHeight().clip(CircleShape)
                            .background(if (active) Color.White else Color.Transparent)
                            .clickable(role = Role.Tab) { onKind(k) }
                            .semantics { selected = active },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (k == AiKind.WEEKLY) "Weekly review" else "Standup",
                            color = if (active) Color(0xFF2E6BE6) else Color.White,
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiLoading(kind: AiKind) {
    val c = Done.colors
    DoneCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 36.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(Modifier.size(32.dp), color = c.accent, strokeWidth = 3.dp)
            Text(
                if (kind == AiKind.WEEKLY) "Reviewing your week…" else "Writing your standup…",
                color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp),
            )
            Text("This takes a few seconds", color = c.muted, fontSize = 13.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeeklyIntro(input: AiInput, onGenerate: () -> Unit) {
    val c = Done.colors
    val zone = java.time.ZoneId.systemDefault()
    val activeDays = input.tasks.map { java.time.Instant.ofEpochMilli(it.completedAt).atZone(zone).toLocalDate() }.distinct().size
    val byCategory = input.tasks.filter { it.category.isNotBlank() }.groupingBy { it.category }.eachCount().entries.sortedByDescending { it.value }

    DoneCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("Your last 7 days", color = c.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("Here's what the review will be written from.", color = c.text2, fontSize = 13.5.sp, modifier = Modifier.padding(top = 2.dp))
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat("${input.count}", "Tasks", Modifier.weight(1f))
                MiniStat("$activeDays/7", "Active days", Modifier.weight(1f))
                MiniStat("${input.tasks.count { it.starred }}", "Starred", Modifier.weight(1f))
            }
            if (byCategory.isNotEmpty()) {
                FlowRow(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    byCategory.forEach { (cat, n) -> CountChip(cat, n) }
                }
            }
            Spacer(Modifier.height(18.dp))
            if (input.count == 0) {
                Text("Log a few tasks first. There's nothing to review yet.", color = c.muted, fontSize = 14.sp)
            } else {
                GenerateButton("Generate weekly review", onGenerate)
            }
        }
    }
    AiFeatureHint(
        listOf("Highlights" to "Your biggest wins", "Where your time went" to "By category", "Patterns" to "Busy and quiet days", "Next week" to "One or two tips"),
    )
}

@Composable
private fun AiFeatureHint(items: List<Pair<String, String>>) {
    val c = Done.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("What you'll get", color = c.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        items.forEach { (title, sub) ->
            val style = sectionStyle(title)
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionBadge(style, 30.dp)
                Spacer(Modifier.width(10.dp))
                Text(title, color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(6.dp))
                Text("· $sub", color = c.muted, fontSize = 13.sp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StandupForm(vm: MainViewModel, input: AiInput, onGenerate: () -> Unit) {
    val c = Done.colors
    val zone = java.time.ZoneId.systemDefault()
    val dayFmt = remember { java.time.format.DateTimeFormatter.ofPattern("EEE, h:mm a") }

    DoneCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("Time period", color = c.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("Only tasks tagged Work are included.", color = c.text2, fontSize = 13.5.sp, modifier = Modifier.padding(top = 2.dp, bottom = 14.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StandupPeriod.entries.forEach { p ->
                    val active = p == vm.standupPeriod
                    Box(
                        Modifier.height(38.dp).clip(CircleShape)
                            .background(if (active) c.accent else c.field)
                            .border(1.dp, if (active) c.accent else c.border, CircleShape)
                            .clickable(role = Role.RadioButton) { vm.standupPeriod = p }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(p.chipLabel(), color = if (active) Color.White else c.text2, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
            Spacer(Modifier.height(14.dp))

            if (input.count == 0) {
                Row {
                    Icon(Icons.Filled.Info, null, tint = c.muted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "No Work tasks ${input.period}. Tag the things you finish at work with the Work category to include them.",
                        color = c.text2, fontSize = 13.5.sp, lineHeight = 19.sp,
                    )
                }
            } else {
                Text(
                    "${input.count} work task${if (input.count == 1) "" else "s"} ${input.period}",
                    color = c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                )
                val work = categoryColors("Work", c.isDark).dot
                input.tasks.take(4).forEach { t ->
                    Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(work))
                        Spacer(Modifier.width(10.dp))
                        Text(t.title, color = c.text2, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(java.time.Instant.ofEpochMilli(t.completedAt).atZone(zone).format(dayFmt), color = c.muted, fontSize = 12.5.sp)
                    }
                }
                if (input.count > 4) Text("+${input.count - 4} more", color = c.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, start = 17.dp))
            }
        }
    }

    DoneCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("For today", color = c.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = vm.standupPlan, onValueChange = { if (it.length <= 1000) vm.standupPlan = it },
                label = { Text("Plans (optional)") }, placeholder = { Text("e.g. Finish the sync review") }, minLines = 2,
                shape = RoundedCornerShape(12.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = vm.standupBlockers, onValueChange = { if (it.length <= 1000) vm.standupBlockers = it },
                label = { Text("Blockers (optional)") }, singleLine = true,
                shape = RoundedCornerShape(12.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (input.count > 0) GenerateButton("Generate standup", onGenerate)
}

@Composable
private fun GenerateButton(text: String, onClick: () -> Unit) {
    GradientButton(
        text = text, onClick = onClick, height = 52.dp, modifier = Modifier.fillMaxWidth(),
        leading = {
            Icon(painterResource(R.drawable.ic_sparkle), null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        },
    )
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier) {
    val c = Done.colors
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(c.field).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(value, color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = c.muted, fontSize = 12.sp)
    }
}

@Composable
private fun CountChip(category: String, count: Int) {
    val cc = categoryColors(category, Done.colors.isDark)
    Row(
        Modifier.clip(CircleShape).background(cc.bg).border(1.dp, cc.border, CircleShape).padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(category, color = cc.text, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(6.dp))
        Text("$count", color = cc.text.copy(alpha = 0.7f), fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
    }
}

/* ---------- AI result as section cards ---------- */

private data class AiSection(val title: String, val items: List<String>, val paragraphs: List<String>)

/** Splits the AI's reply into sections on "## Title" (review) or "*Title*" (Slack standup) lines. */
private fun parseSections(text: String): List<AiSection> {
    val out = mutableListOf<AiSection>()
    var title = ""
    var items = mutableListOf<String>()
    var paras = mutableListOf<String>()
    fun flush() {
        if (title.isNotEmpty() || items.isNotEmpty() || paras.isNotEmpty()) out += AiSection(title, items, paras)
        items = mutableListOf(); paras = mutableListOf()
    }
    text.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
        val heading = Regex("^#{1,4}\\s+(.*)").find(line)?.groupValues?.get(1)
            ?: Regex("^\\*([^*]+)\\*:?$").find(line)?.groupValues?.get(1)
        val bullet = Regex("^[-•*]\\s+(.*)").find(line)?.groupValues?.get(1)
        when {
            heading != null -> { flush(); title = heading.trim() }
            bullet != null -> items += bullet
            else -> paras += line
        }
    }
    flush()
    return out
}

private data class SectionStyle(val icon: @Composable () -> Painter, val tint: Color)

@Composable
private fun sectionStyle(title: String): SectionStyle {
    val t = title.lowercase()
    return when {
        "highlight" in t -> SectionStyle({ rememberVectorPainter(Icons.Filled.Star) }, Color(0xFFF59E0B))
        "time went" in t || "where" in t -> SectionStyle({ rememberVectorPainter(Icons.Filled.DateRange) }, Color(0xFF3B7BF6))
        "pattern" in t -> SectionStyle({ painterResource(R.drawable.ic_sparkle) }, Color(0xFF8B5CF6))
        "next" in t -> SectionStyle({ rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowForward) }, Color(0xFF14B8A6))
        "blocker" in t -> SectionStyle({ rememberVectorPainter(Icons.Filled.Warning) }, Color(0xFFF97316))
        t == "today" -> SectionStyle({ rememberVectorPainter(Icons.Filled.PlayArrow) }, Color(0xFF3B7BF6))
        else -> SectionStyle({ rememberVectorPainter(Icons.Filled.CheckCircle) }, GoalGreen)
    }
}

@Composable
private fun SectionBadge(style: SectionStyle, size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(style.tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
        Icon(style.icon(), null, tint = style.tint, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
private fun AiResult(vm: MainViewModel, kind: AiKind, text: String, input: AiInput) {
    val c = Done.colors
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var copied by remember(text) { mutableIntStateOf(0) }
    val sections = remember(text) { parseSections(text) }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(if (kind == AiKind.WEEKLY) "Your weekly review" else "Your standup", color = c.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("From ${input.count} task${if (input.count == 1) "" else "s"} · ${input.period}", color = c.muted, fontSize = 13.sp)
        }
        if (kind == AiKind.STANDUP) TextButton(onClick = { vm.resetAi(kind) }) { Text("Edit", color = c.accentText, fontWeight = FontWeight.SemiBold) }
        IconButton(onClick = { vm.generateAi(kind) }) { Icon(Icons.Filled.Refresh, "Regenerate", tint = c.text2) }
    }

    sections.forEach { s ->
        val style = sectionStyle(s.title)
        DoneCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                if (s.title.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionBadge(style, 34.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(s.title, color = c.text, fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                }
                s.paragraphs.forEach { Text(bold(it), color = c.text2, fontSize = 14.5.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 6.dp)) }
                s.items.forEach { item ->
                    Row(Modifier.padding(top = 8.dp)) {
                        Box(Modifier.padding(top = 7.dp).size(6.dp).clip(CircleShape).background(style.tint))
                        Spacer(Modifier.width(12.dp))
                        Text(bold(item), color = c.text2, fontSize = 14.5.sp, lineHeight = 21.sp)
                    }
                }
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
        Row(
            Modifier.weight(1f).height(50.dp).clip(CircleShape).background(c.accentSoft)
                .clickable(role = Role.Button) {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                    context.startActivity(Intent.createChooser(send, null))
                },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Filled.Share, null, tint = c.accentText, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Share", color = c.accentText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        GradientButton(
            text = if (copied > 0) "Copied" else "Copy", height = 50.dp, modifier = Modifier.weight(1f),
            onClick = { scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Ticked", text))); copied++ } },
        )
    }
}
