package com.donetimeline.app.ui

import android.content.ClipData
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

/** Header used by the Goals and AI tabs (same look as Home's). */
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

/* ------------------------------------------------------------------ Goals tab */

private data class GoalTarget(val goal: Goal?)

@Composable
fun GoalsScreen(vm: MainViewModel, onOpenSettings: () -> Unit) {
    val c = Done.colors
    val state by vm.state.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<GoalTarget?>(null) }
    val goals = state.goals
    val subtitle = if (goals.isEmpty()) "Set targets and track them as you log" else "${goals.count { it.met }} of ${goals.size} on track"

    Column(Modifier.fillMaxSize().background(c.bg)) {
        TabTopBar("Goals", subtitle, onOpenSettings)
        Box(Modifier.weight(1f)) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (goals.isEmpty()) {
                    item { EmptyGoals { editor = GoalTarget(null) } }
                } else {
                    items(goals, key = { it.goal.id }) { p -> GoalRow(p) { editor = GoalTarget(p.goal) } }
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

@Composable
private fun GoalRow(p: GoalProgress, onClick: () -> Unit) {
    val c = Done.colors
    val barColor = if (p.met) GoalGreen else categoryColors(p.goal.category, c.isDark).dot
    DoneCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            GoalRing(p, 54.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(p.goal.name, color = c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("${p.goal.target} ${p.goal.period.per}", color = c.text2, fontSize = 13.5.sp)
                Box(
                    Modifier.padding(top = 10.dp).fillMaxWidth().height(6.dp).clip(CircleShape).background(c.hover),
                ) {
                    Box(Modifier.fillMaxWidth(p.ratio.coerceAtLeast(0.02f)).fillMaxHeight().clip(CircleShape).background(barColor))
                }
                Text(
                    if (p.met) "Goal met ${p.goal.period.now}" else "${p.done} of ${p.goal.target} ${p.goal.period.now}",
                    color = if (p.met) GoalGreen else c.muted, fontSize = 12.5.sp,
                    fontWeight = if (p.met) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyGoals(onAdd: () -> Unit) {
    val c = Done.colors
    DoneCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(c.accentSoft), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_target), null, tint = c.accentText, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("No goals yet", color = c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Set a target like Health · 4 per week. It fills up automatically as you log tasks.",
                color = c.text2, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            GradientButton("Add your first goal", onClick = onAdd, height = 44.dp)
        }
    }
}

/* ------------------------------------------------------------------ AI tab */

@Composable
fun AiScreen(vm: MainViewModel, onOpenSettings: () -> Unit) {
    val c = Done.colors
    val state by vm.state.collectAsStateWithLifecycle()
    var kind by rememberSaveable { mutableStateOf(AiKind.WEEKLY) }
    val input = remember(kind, state.allTasks) { vm.aiInput(kind) }
    val text = vm.aiResults[kind]
    val busy = vm.aiBusy[kind] == true
    val error = vm.aiErrors[kind]

    Column(Modifier.fillMaxSize().background(c.bg)) {
        TabTopBar("Ticked AI", "Summaries from what you've finished", onOpenSettings)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Tabs(
                options = AiKind.entries, selected = kind,
                label = { if (it == AiKind.WEEKLY) "Weekly review" else "Standup" },
                count = { null }, onSelect = { kind = it }, modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Based on ${input.count} task${if (input.count == 1) "" else "s"} from ${input.period}.",
                color = c.muted, fontSize = 13.sp,
            )

            when {
                busy -> DoneCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = c.accent, strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(if (kind == AiKind.WEEKLY) "Reviewing your week…" else "Writing your standup…", color = c.text2, fontSize = 14.5.sp)
                    }
                }
                text != null -> AiResult(vm, kind, text)
                kind == AiKind.WEEKLY -> AiIntro(
                    "Your week in review",
                    "Highlights, where your time went, patterns and a tip for next week, written from the last 7 days of tasks.",
                    "Generate weekly review",
                ) { vm.generateAi(kind) }
                else -> StandupForm(vm) { vm.generateAi(kind) }
            }
            error?.let { Text(it, color = c.danger, fontSize = 13.5.sp) }
            Text("AI can make mistakes. Your task list is sent to Groq only to write this text.", color = c.muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AiIntro(title: String, body: String, action: String, onGenerate: () -> Unit) {
    val c = Done.colors
    DoneCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(c.accentSoft), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_sparkle), null, tint = c.accentText, modifier = Modifier.size(24.dp))
            }
            Text(title, color = c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))
            Text(body, color = c.text2, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))
            GradientButton(action, onClick = onGenerate, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun StandupForm(vm: MainViewModel, onGenerate: () -> Unit) {
    val c = Done.colors
    DoneCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Daily standup", color = c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Yesterday is filled in from your tasks. Add today's plan and any blockers, and get a Slack-ready update.",
                color = c.text2, fontSize = 14.sp, lineHeight = 20.sp,
            )
            OutlinedTextField(
                value = vm.standupPlan, onValueChange = { if (it.length <= 1000) vm.standupPlan = it },
                label = { Text("Plans for today (optional)") }, minLines = 2,
                shape = RoundedCornerShape(12.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = vm.standupBlockers, onValueChange = { if (it.length <= 1000) vm.standupBlockers = it },
                label = { Text("Blockers (optional)") }, singleLine = true,
                shape = RoundedCornerShape(12.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
            )
            GradientButton("Generate standup", onClick = onGenerate, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun AiResult(vm: MainViewModel, kind: AiKind, text: String) {
    val c = Done.colors
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var copied by remember(text) { mutableIntStateOf(0) }

    DoneCard(Modifier.fillMaxWidth()) {
        Box(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) { RichText(text) }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (kind == AiKind.STANDUP) {
            TextButton(onClick = { vm.resetAi(kind) }) { Text("Edit", color = c.text2) }
        }
        TextButton(onClick = { vm.generateAi(kind) }) {
            Icon(Icons.Filled.Refresh, null, tint = c.text2, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Regenerate", color = c.text2)
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(c.accentSoft).border(0.dp, Color.Transparent, CircleShape)
                .clickable(role = Role.Button) {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                    context.startActivity(Intent.createChooser(send, null))
                },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Share, "Share", tint = c.accentText, modifier = Modifier.size(20.dp)) }
        GradientButton(
            text = if (copied > 0) "Copied" else "Copy", height = 44.dp,
            onClick = { scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Ticked", text))); copied++ } },
        )
    }
}
