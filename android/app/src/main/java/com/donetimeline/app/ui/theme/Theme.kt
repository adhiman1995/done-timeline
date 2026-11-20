package com.donetimeline.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** A soft badge tone: background, text and dot colours. */
@Immutable
data class Tone(val bg: Color, val text: Color, val dot: Color)

/** Design tokens shared with the web app (client/src/styles.css). */
@Immutable
data class DoneColors(
    val isDark: Boolean,
    val bg: Color,
    /** Grey container that groups white cards. */
    val panel: Color,
    /** Card background. */
    val surface: Color,
    val surfaceSolid: Color,
    /** Hairline card border. */
    val border: Color,
    val borderStrong: Color,
    val text: Color,
    val text2: Color,
    val muted: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentText: Color,
    val onAccent: Color,
    val star: Color,
    val danger: Color,
    val rail: Color,
    val field: Color,
    val hover: Color,
    val countBg: Color,
    val today: Tone,
    val week: Tone,
    val older: Tone,
) {
    val gradient get() = Brush.linearGradient(listOf(Color(0xFF4B8BFF), Color(0xFF2E6BE6)))
    val logoGradient get() = Brush.linearGradient(listOf(Color(0xFF5B9BFF), Color(0xFF2E6BE6)))
}

val LightTokens = DoneColors(
    isDark = false,
    bg = Color(0xFFF5F6F8),
    panel = Color(0xFFECEFF3),
    surface = Color.White,
    surfaceSolid = Color.White,
    border = Color(0xFFEBEEF2),
    borderStrong = Color(0xFFE3E7ED),
    text = Color(0xFF101828),
    text2 = Color(0xFF4B5565),
    muted = Color(0xFF8A94A6),
    accent = Color(0xFF3B7BF6),
    accentSoft = Color(0xFFE7EFFE),
    accentText = Color(0xFF2563EB),
    onAccent = Color.White,
    star = Color(0xFFF5A524),
    danger = Color(0xFFC3373C),
    rail = Color(0xFFE3E7ED),
    field = Color(0xFFF4F6F9),
    hover = Color(0xFFEEF1F5),
    countBg = Color(0xFFE6E9EF),
    today = Tone(Color(0xFFFDF0E1), Color(0xFFB4621A), Color(0xFFF59E0B)),
    week = Tone(Color(0xFFE7EFFE), Color(0xFF2563EB), Color(0xFF3B7BF6)),
    older = Tone(Color(0xFFEEF0F3), Color(0xFF5B6574), Color(0xFFA3ABB8)),
)

val DarkTokens = DoneColors(
    isDark = true,
    bg = Color(0xFF0B0F16),
    panel = Color(0xFF111620),
    surface = Color(0xFF171E2A),
    surfaceSolid = Color(0xFF171E2A),
    border = Color(0xFF1E2633),
    borderStrong = Color(0xFF252E3D),
    text = Color(0xFFE9EDF4),
    text2 = Color(0xFFA6B0C0),
    muted = Color(0xFF6F7A8D),
    accent = Color(0xFF4D8BF7),
    accentSoft = Color(0x264D8BF7),
    accentText = Color(0xFF85B4FF),
    onAccent = Color.White,
    star = Color(0xFFF7B73A),
    danger = Color(0xFFFF8B8F),
    rail = Color(0xFF252E3D),
    field = Color(0xFF10151E),
    hover = Color(0x0DFFFFFF),
    countBg = Color(0x14FFFFFF),
    today = Tone(Color(0x24F59E0B), Color(0xFFF4B55A), Color(0xFFF59E0B)),
    week = Tone(Color(0x264D8BF7), Color(0xFF8FBAFF), Color(0xFF4D8BF7)),
    older = Tone(Color(0x0FFFFFFF), Color(0xFFA6B0C0), Color(0xFF6F7A8D)),
)

val LocalDoneColors = staticCompositionLocalOf { LightTokens }

object Done {
    val colors: DoneColors
        @Composable get() = LocalDoneColors.current
}

// Category → hue, matching client/src/colors.js.
private val presetHues = mapOf(
    "work" to 262f, "learning" to 189f, "study" to 189f, "health" to 152f, "fitness" to 152f,
    "personal" to 38f, "home" to 24f, "finance" to 330f, "family" to 345f, "side" to 212f,
)
private val paletteHues = listOf(212f, 262f, 189f, 152f, 38f, 330f, 12f, 290f)

fun categoryHue(name: String): Float {
    if (name.isBlank()) return 217f
    val key = name.trim().lowercase()
    presetHues[key]?.let { return it }
    var h = 0L
    for (ch in key) h = (h * 31 + ch.code) and 0xFFFFFFFFL
    return paletteHues[(h % paletteHues.size).toInt()]
}

data class CategoryColors(val dot: Color, val text: Color, val bg: Color, val border: Color)

/** Same formulas as the web's .chip / .cat-icon / dots. */
fun categoryColors(name: String, dark: Boolean): CategoryColors {
    val hue = categoryHue(name)
    return if (dark) CategoryColors(
        dot = Color.hsl(hue, 0.72f, 0.60f),
        text = Color.hsl(hue, 0.85f, 0.76f),
        bg = Color.hsl(hue, 0.45f, 0.22f, alpha = 0.6f),
        border = Color.hsl(hue, 0.45f, 0.35f, alpha = 0.6f),
    ) else CategoryColors(
        dot = Color.hsl(hue, 0.72f, 0.56f),
        text = Color.hsl(hue, 0.55f, 0.40f),
        bg = Color.hsl(hue, 0.85f, 0.95f),
        border = Color.hsl(hue, 0.70f, 0.89f),
    )
}

private val typography = Typography().run {
    copy(
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
    )
}

@Composable
fun DoneTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val t = if (dark) DarkTokens else LightTokens
    val scheme = if (dark) {
        darkColorScheme(
            primary = t.accent, onPrimary = t.onAccent,
            background = t.bg, onBackground = t.text,
            surface = t.surface, onSurface = t.text,
            surfaceVariant = t.panel, onSurfaceVariant = t.text2,
            surfaceContainer = t.surface, surfaceContainerHigh = t.surface,
            surfaceContainerHighest = t.panel, surfaceContainerLow = t.surface,
            outline = t.borderStrong, outlineVariant = t.border,
            error = t.danger,
        )
    } else {
        lightColorScheme(
            primary = t.accent, onPrimary = t.onAccent,
            background = t.bg, onBackground = t.text,
            surface = t.surface, onSurface = t.text,
            surfaceVariant = t.panel, onSurfaceVariant = t.text2,
            surfaceContainer = Color.White, surfaceContainerHigh = Color.White,
            surfaceContainerHighest = t.panel, surfaceContainerLow = Color.White,
            outline = t.borderStrong, outlineVariant = t.border,
            error = t.danger,
        )
    }
    CompositionLocalProvider(LocalDoneColors provides t) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}

val LabelCaps = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.9.sp)
