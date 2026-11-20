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

/** Design tokens shared with the web app (client/src/styles.css). */
@Immutable
data class DoneColors(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surfaceSolid: Color,
    val border: Color,
    val borderStrong: Color,
    val text: Color,
    val text2: Color,
    val muted: Color,
    val accent: Color,
    val accentText: Color,
    val onAccent: Color,
    val star: Color,
    val danger: Color,
    val rail: Color,
    val field: Color,
    val hover: Color,
) {
    val gradient get() = Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF5B5FF0)))
    val logoGradient get() = Brush.linearGradient(listOf(Color(0xFF60A5FA), Color(0xFF5B5FF0)))
}

val DarkTokens = DoneColors(
    isDark = true,
    bg = Color(0xFF080C18),
    surface = Color(0xFF111A2D),
    surfaceSolid = Color(0xFF0F1729),
    border = Color(0x1F7896DC),
    borderStrong = Color(0x3D78A0FF),
    text = Color(0xFFE8EEFC),
    text2 = Color(0xFFA3B0CC),
    muted = Color(0xFF6F7C9A),
    accent = Color(0xFF3B82F6),
    accentText = Color(0xFF7FB0FF),
    onAccent = Color.White,
    star = Color(0xFFFBBF24),
    danger = Color(0xFFF87171),
    rail = Color(0x593B82F6),
    field = Color(0x09FFFFFF),
    hover = Color(0x0FFFFFFF),
)

val LightTokens = DoneColors(
    isDark = false,
    bg = Color(0xFFF4F6FB),
    surface = Color.White,
    surfaceSolid = Color.White,
    border = Color(0x171E3264),
    borderStrong = Color(0x3D3B6EDC),
    text = Color(0xFF142038),
    text2 = Color(0xFF4B5876),
    muted = Color(0xFF8591AB),
    accent = Color(0xFF3B82F6),
    accentText = Color(0xFF2563EB),
    onAccent = Color.White,
    star = Color(0xFFF59E0B),
    danger = Color(0xFFDC2626),
    rail = Color(0x403B82F6),
    field = Color.White,
    hover = Color(0x0F1E3264),
)

val LocalDoneColors = staticCompositionLocalOf { DarkTokens }

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

fun categoryColors(name: String, dark: Boolean): CategoryColors {
    val hue = categoryHue(name)
    val dot = Color.hsl(hue, 0.80f, if (dark) 0.62f else 0.50f)
    return CategoryColors(
        dot = dot,
        text = Color.hsl(hue, 0.75f, if (dark) 0.76f else 0.36f),
        bg = Color.hsl(hue, 0.80f, 0.58f, alpha = if (dark) 0.13f else 0.10f),
        border = Color.hsl(hue, 0.80f, 0.60f, alpha = 0.22f),
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
            surface = t.surfaceSolid, onSurface = t.text,
            surfaceVariant = t.surface, onSurfaceVariant = t.text2,
            surfaceContainer = t.surface, surfaceContainerHigh = Color(0xFF16213A),
            surfaceContainerHighest = Color(0xFF1B2742), surfaceContainerLow = t.surfaceSolid,
            outline = t.borderStrong, outlineVariant = t.border,
            error = t.danger,
        )
    } else {
        lightColorScheme(
            primary = t.accent, onPrimary = t.onAccent,
            background = t.bg, onBackground = t.text,
            surface = t.surfaceSolid, onSurface = t.text,
            surfaceVariant = Color(0xFFEEF2FA), onSurfaceVariant = t.text2,
            surfaceContainer = Color.White, surfaceContainerHigh = Color.White,
            surfaceContainerHighest = Color(0xFFEEF2FA), surfaceContainerLow = Color.White,
            outline = t.borderStrong, outlineVariant = t.border,
            error = t.danger,
        )
    }
    CompositionLocalProvider(LocalDoneColors provides t) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}

val LabelCaps = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.9.sp)
