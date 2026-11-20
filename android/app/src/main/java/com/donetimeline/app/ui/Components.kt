package com.donetimeline.app.ui

import androidx.compose.foundation.BorderStroke
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
        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.55f))
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
    val shape = RoundedCornerShape(14.dp)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = c.surface, border = BorderStroke(1.dp, c.border), content = content)
    } else {
        Surface(modifier = modifier, shape = shape, color = c.surface, border = BorderStroke(1.dp, c.border), content = content)
    }
}

@Composable
fun CategoryChip(name: String, modifier: Modifier = Modifier, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    val cc = categoryColors(name, Done.colors.isDark)
    Row(
        modifier
            .clip(RoundedCornerShape(7.dp))
            .background(cc.bg)
            .border(if (selected) 2.dp else 1.dp, if (selected) cc.dot else cc.border, RoundedCornerShape(7.dp))
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(cc.dot))
        Spacer(Modifier.width(6.dp))
        Text(name, color = cc.text, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/** Pill with a count badge, used for the category / starred filters. */
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
    val bg = if (selected) c.accent else c.field
    val fg = if (selected) c.onAccent else c.text2
    Row(
        Modifier
            .height(34.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, if (selected) c.accent else c.border, CircleShape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(start = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dotColor != null) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(if (selected) Color.White else dotColor))
            Spacer(Modifier.width(7.dp))
        }
        if (starred) {
            Icon(Icons.Filled.Star, null, tint = if (selected) Color.White else c.star, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        Spacer(Modifier.width(7.dp))
        Box(
            Modifier
                .heightIn(min = 22.dp)
                .clip(CircleShape)
                .background(if (selected) Color.White.copy(alpha = 0.22f) else c.hover)
                .padding(horizontal = 7.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("$count", color = if (selected) Color.White else c.muted, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
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
