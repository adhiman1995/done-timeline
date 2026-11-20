package com.donetimeline.app.ui

import androidx.compose.foundation.BorderStroke
import com.donetimeline.app.ui.theme.Tone
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.donetimeline.app.ui.theme.Done
import com.donetimeline.app.ui.theme.categoryColors

@Composable
fun AppLogo(size: Dp = 40.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(Done.colors.logoGradient),
        contentAlignment = Alignment.Center,
    ) {
        TickedMark(Modifier.size(size * 0.72f), Color.White)
    }
}

/** Ring with a tick breaking out through its gap; drawn in a 64-unit box like the web icon. */
@Composable
fun TickedMark(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier) {
        val u = size.minDimension / 64f
        val dx = -1.2f * u
        val dy = 1f * u
        val stroke = Stroke(width = 5.4f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val r = 15.5f * u
        val center = Offset(32f * u + dx, 32f * u + dy)
        // Gap between -70° and -5° (top right), in degrees clockwise from 3 o'clock.
        drawArc(
            color, startAngle = -5f, sweepAngle = 295f, useCenter = false,
            topLeft = Offset(center.x - r, center.y - r), size = Size(r * 2, r * 2), style = stroke,
        )
        val tick = Path().apply {
            moveTo(23.8f * u + dx, 31.6f * u + dy)
            lineTo(30.6f * u + dx, 38.4f * u + dy)
            lineTo(48.5f * u + dx, 18.5f * u + dy)
        }
        drawPath(tick, color, style = stroke)
    }
}

/** Rounded, bordered surface used for cards across the app. */
@Composable
fun DoneCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val c = Done.colors
    val shape = RoundedCornerShape(16.dp)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = c.surface, border = BorderStroke(1.dp, c.border), content = content)
    } else {
        Surface(modifier = modifier, shape = shape, color = c.surface, border = BorderStroke(1.dp, c.border), content = content)
    }
}

@Composable
fun CategoryChip(name: String, modifier: Modifier = Modifier, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    val cc = categoryColors(name, Done.colors.isDark)
    Box(
        modifier
            .height(if (onClick != null) 32.dp else 24.dp)
            .clip(CircleShape)
            .background(if (selected) cc.dot else cc.bg)
            .border(1.dp, if (selected) cc.dot else cc.border, CircleShape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = if (onClick != null) 13.dp else 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(name, color = if (selected) Color.White else cc.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Filter chip with a count, used for the category / starred filters. */
@Composable
fun FilterPill(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    dotColor: Color? = null,
    starred: Boolean = false,
) {
    val c = Done.colors
    Row(
        Modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(if (selected) c.accentSoft else c.surface)
            .border(1.dp, if (selected) c.accent else c.border, CircleShape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dotColor != null) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor))
            Spacer(Modifier.width(7.dp))
        }
        if (starred) {
            Icon(Icons.Filled.Star, null, tint = c.star, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, color = if (selected) c.accentText else c.text2, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        Spacer(Modifier.width(8.dp))
        CountBadge(count, selected)
    }
}

@Composable
fun CountBadge(count: Int, highlighted: Boolean = false) {
    val c = Done.colors
    Box(
        Modifier
            .heightIn(min = 22.dp)
            .widthIn(min = 22.dp)
            .clip(CircleShape)
            .background(if (highlighted) c.accentSoft else c.countBg)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("$count", color = if (highlighted) c.accentText else c.text2, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Soft circle with the category's initial (or a tick when there's no category). */
@Composable
fun CategoryIcon(category: String, size: Dp = 38.dp) {
    val cc = categoryColors(category, Done.colors.isDark)
    Box(
        Modifier.size(size).clip(CircleShape).background(cc.bg),
        contentAlignment = Alignment.Center,
    ) {
        if (category.isBlank()) {
            Icon(Icons.Filled.Check, null, tint = cc.text, modifier = Modifier.size(size * 0.48f))
        } else {
            Text(category.trim().first().uppercase(), color = cc.text, fontSize = (size.value * 0.4f).sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Soft pill with a dot, e.g. "Today" (amber) / "Yesterday" (blue) / older dates (grey). */
@Composable
fun ToneBadge(text: String, tone: Tone) {
    Row(
        Modifier.height(28.dp).clip(CircleShape).background(tone.bg).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(tone.dot))
        Spacer(Modifier.width(7.dp))
        Text(text, color = tone.text, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Tabs on a grey panel; the selected tab is a white pill (like the web's range tabs). */
@Composable
fun <T> Tabs(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    count: (T) -> Int?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Done.colors
    Row(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(c.panel)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { o ->
            val active = o == selected
            Row(
                Modifier
                    .weight(1f)
                    .height(38.dp)
                    .shadow(if (active) 1.dp else 0.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(if (active) c.surface else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(o) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    label(o),
                    color = if (active) c.text else c.text2,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                count(o)?.let {
                    Spacer(Modifier.width(5.dp))
                    CountBadge(it, highlighted = active)
                }
            }
        }
    }
}

@Composable
fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val c = Done.colors
    Row(
        modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(c.field)
            .border(1.dp, c.border, RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { o ->
            val active = o == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (active) c.accent else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(o) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(o),
                    color = if (active) c.onAccent else c.text2,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Primary pill button with the brand gradient. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 48.dp,
    leading: (@Composable RowScope.() -> Unit)? = null,
) {
    val c = Done.colors
    Row(
        modifier
            .height(height)
            .clip(CircleShape)
            .background(if (enabled) c.gradient else androidx.compose.ui.graphics.SolidColor(c.muted.copy(alpha = 0.4f)))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        leading?.invoke(this)
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier = modifier, color = Done.colors.text2, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.9.sp)
}

@Composable
fun fieldColors() = Done.colors.let { c ->
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = c.accent,
        unfocusedBorderColor = c.border,
        focusedContainerColor = c.field,
        unfocusedContainerColor = c.field,
        focusedTextColor = c.text,
        unfocusedTextColor = c.text,
        focusedLabelColor = c.accentText,
        unfocusedLabelColor = c.muted,
        unfocusedPlaceholderColor = c.muted,
        focusedPlaceholderColor = c.muted,
        cursorColor = c.accent,
    )
}

/** "just now", "5 minutes ago", "Yesterday"… */
fun relativeTime(epochMillis: Long): String {
    val now = System.currentTimeMillis()
    if (now - epochMillis < 60_000) return "just now"
    return android.text.format.DateUtils.getRelativeTimeSpanString(
        epochMillis, now, android.text.format.DateUtils.MINUTE_IN_MILLIS,
    ).toString().replaceFirstChar { it.lowercase() }
}

/** Lets a child (e.g. a horizontally scrolling row) extend past its parent's horizontal padding. */
fun Modifier.bleed(horizontal: Dp) = this.then(
    Modifier.layout { measurable, constraints ->
        val extra = (horizontal * 2).roundToPx()
        val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + extra, minWidth = constraints.minWidth + extra))
        layout(placeable.width - extra, placeable.height) { placeable.place(-horizontal.roundToPx(), 0) }
    }
)

@Composable
fun StatusLine(text: String, color: Color = Done.colors.muted) {
    Column { Text(text, color = color, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}
