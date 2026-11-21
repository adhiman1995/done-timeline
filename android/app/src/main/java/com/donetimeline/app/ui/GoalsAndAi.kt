package com.donetimeline.app.ui

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.donetimeline.app.R
import com.donetimeline.app.data.AiKind
import com.donetimeline.app.data.Goal
import com.donetimeline.app.data.GoalPeriod
import com.donetimeline.app.data.GoalProgress
import com.donetimeline.app.ui.theme.Done
import com.donetimeline.app.ui.theme.categoryColors
import kotlinx.coroutines.launch

val GoalGreen = Color(0xFF22A06B)

@Composable
private fun SoftButton(text: String, modifier: Modifier = Modifier, icon: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    val c = Done.colors
    Row(
        modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(c.accentSoft)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        icon?.let { it(); Spacer(Modifier.width(6.dp)) }
        Text(text, color = c.accentText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Renders the AI's answer: "## heading", "- / •" bullets, **bold** and Slack-style *Heading* lines. */
@Composable
fun RichText(text: String) {
    val c = Done.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        text.lines().forEachIndexed { i, raw ->
            val line = raw.trim()
            when {
                line.isEmpty() -> Unit
                line.startsWith("#") || Regex("^\\*[^*]+\\*$").matches(line) -> Text(
                    line.trimStart('#', ' ').trim('*'),
                    color = c.text, fontSize = 14.5.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = if (i == 0) 0.dp else 8.dp),
                )
                Regex("^[-•*]\\s+.*").matches(line) -> Row {
                    Text("•", color = c.text2, fontSize = 14.5.sp, modifier = Modifier.width(16.dp))
                    Text(bold(line.drop(1).trim()), color = c.text2, fontSize = 14.5.sp, lineHeight = 21.sp)
                }
                else -> Text(bold(line), color = c.text2, fontSize = 14.5.sp, lineHeight = 21.sp)
            }
        }
    }
}

@Composable
private fun bold(s: String): AnnotatedString {
    val c = Done.colors
    return buildAnnotatedString {
        s.split("**").forEachIndexed { i, part ->
            if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = c.text)) { append(part) } else append(part)
        }
    }
}

/* ------------------------------------------------------------------ Goals */

@Composable
fun GoalRing(p: GoalProgress, size: Dp) {
    val c = Done.colors
    val color = if (p.met) GoalGreen else categoryColors(p.goal.category, c.isDark).dot
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = 5.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(c.hover, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(color, -90f, 360f * p.ratio, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        if (p.met) Icon(Icons.Filled.Check, null, tint = GoalGreen, modifier = Modifier.size(20.dp))
        else Text("${p.done}", color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun GoalSheet(
    goal: Goal?,
    categories: List<String>,
    onSave: (category: String, target: Int, period: GoalPeriod) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val c = Done.colors
    var category by remember { mutableStateOf(goal?.category ?: "") }
    var target by remember { mutableIntStateOf(goal?.target ?: 3) }
    var period by remember { mutableStateOf(goal?.period ?: GoalPeriod.WEEK) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surfaceSolid,
        contentColor = c.text,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(if (goal == null) "New goal" else "Edit goal", color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)

            SectionLabel("What counts")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf("") + categories).forEach { cat ->
                    CategoryChip(
                        if (cat.isEmpty()) "Any task" else cat,
                        selected = category.equals(cat, ignoreCase = true),
                        onClick = { category = cat },
                    )
                }
            }
            OutlinedTextField(
                value = category, onValueChange = { if (it.length <= 40) category = it },
                label = { Text("Or type a category") }, singleLine = true,
                shape = RoundedCornerShape(12.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel("Target")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StepButton("−") { target = (target - 1).coerceAtLeast(1) }
                OutlinedTextField(
                    value = "$target",
                    onValueChange = { v -> v.filter(Char::isDigit).take(3).toIntOrNull()?.let { target = it.coerceIn(1, 100) } },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp), colors = fieldColors(), modifier = Modifier.width(88.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = c.text),
                )
                StepButton("+") { target = (target + 1).coerceAtMost(100) }
            }
            Tabs(
                options = GoalPeriod.entries, selected = period, label = { "Every ${it.key}" },
                count = { null }, onSelect = { period = it }, modifier = Modifier.fillMaxWidth(),
            )

            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.accentSoft).padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text("${category.ifBlank { "Any task" }} · $target ${period.per}", color = c.accentText, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete", color = c.danger, fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancel", color = c.text2) }
                Spacer(Modifier.width(6.dp))
                GradientButton(
                    text = if (goal == null) "Add goal" else "Save",
                    height = 44.dp,
                    onClick = { onSave(category.trim(), target, period) },
                )
            }
        }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    val c = Done.colors
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(c.field).border(1.dp, c.border, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = c.text, fontSize = 22.sp) }
}
