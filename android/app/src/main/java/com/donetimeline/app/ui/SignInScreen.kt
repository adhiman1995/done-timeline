package com.donetimeline.app.ui

import android.app.Activity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.donetimeline.app.R
import com.donetimeline.app.ui.theme.Done
import com.donetimeline.app.ui.theme.LightTokens
import com.donetimeline.app.ui.theme.LocalDoneColors
import kotlinx.coroutines.launch

private val BrandTop = Color(0xFF5B9BFF)
private val BrandBottom = Color(0xFF2E6BE6)
private val BrandMid = Color(0xFF4483F3) // same as the launch splash (@color/brand_blue)

/**
 * Welcome + Google sign-in. Picks up from the launch splash (same brand blue and mark),
 * then animates in the brand, a small timeline preview and the sign-in button.
 */
@Composable
fun SignInScreen(vm: MainViewModel) {
    val context = LocalContext.current // the Activity: Credential Manager needs it to show the account picker
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    LightSystemBarIcons()

    // Entrance animation: 0 → 1 shortly after the splash hands over.
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val brandIn by animateFloatAsState(if (entered) 1f else 0f, tween(650, easing = FastOutSlowInEasing), label = "brand")
    val actionsIn by animateFloatAsState(if (entered) 1f else 0f, tween(600, delayMillis = 450, easing = FastOutSlowInEasing), label = "actions")

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BrandMid, BrandTop, BrandBottom))),
    ) {
        // Soft light blooms for depth.
        Box(
            Modifier.size(maxWidth * 0.95f).offset(x = maxWidth * 0.45f, y = -maxWidth * 0.3f)
                .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.20f), Color.Transparent)), CircleShape),
        )
        Box(
            Modifier.size(maxWidth * 0.9f).offset(x = -maxWidth * 0.4f, y = maxHeight * 0.55f)
                .background(Brush.radialGradient(listOf(Color(0xFF9CC2FF).copy(alpha = 0.25f), Color.Transparent)), CircleShape),
        )

        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.9f))

            // Brand
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = 0.3f + 0.7f * brandIn
                    translationY = (1f - brandIn) * 28.dp.toPx()
                },
            ) {
                Box(
                    Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    TickedMark(Modifier.size(64.dp), Color.White)
                }
                Spacer(Modifier.height(22.dp))
                Text("Ticked", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Everything you've finished,\non one timeline.",
                    color = Color.White.copy(alpha = 0.88f), fontSize = 16.5.sp, lineHeight = 23.sp, textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(36.dp))
            TimelinePreview(entered)
            Spacer(Modifier.weight(1.1f))

            // Actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().graphicsLayer {
                    alpha = actionsIn
                    translationY = (1f - actionsIn) * 24.dp.toPx()
                },
            ) {
                error?.let {
                    Text(
                        it,
                        color = Color.White, fontSize = 13.5.sp, lineHeight = 19.sp, textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x33000000))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
                GoogleButton(busy) {
                    scope.launch {
                        busy = true
                        error = null
                        error = vm.signIn(context)
                        busy = false
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "Works offline · Syncs with the Ticked website",
                    color = Color.White.copy(alpha = 0.78f), fontSize = 13.sp,
                )
                Text(
                    "Privacy Policy",
                    color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { uriHandler.openUri(PRIVACY_URL) }.padding(8.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/** White "Continue with Google" button with the Google G, per Google's sign-in branding. */
@Composable
private fun GoogleButton(busy: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(16.dp, CircleShape, ambientColor = Color(0xFF0B2A6B), spotColor = Color(0xFF0B2A6B))
            .clip(CircleShape)
            .background(Color.White)
            .clickable(enabled = !busy, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(20.dp), color = BrandBottom, strokeWidth = 2.dp)
        } else {
            Image(painterResource(R.drawable.ic_google), contentDescription = null, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            if (busy) "Signing in…" else "Continue with Google",
            color = Color(0xFF1F1F1F), fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
        )
    }
}

private data class PreviewItem(val title: String, val category: String, val time: String)

// Solid cards that tint towards the brand blue further down the stack (translucency would expose the shadows).
private val previewCardColors = listOf(Color.White, Color(0xFFEFF4FF), Color(0xFFDCE7FE))

private val previewItems = listOf(
    PreviewItem("Shipped the onboarding flow", "Work", "9:30"),
    PreviewItem("Morning run · 5 km", "Health", "8:00"),
    PreviewItem("Read chapter 4", "Learning", "7:15"),
)

/** Three mini timeline cards that slide in one after another (always drawn in the light palette). */
@Composable
private fun TimelinePreview(entered: Boolean) {
    CompositionLocalProvider(LocalDoneColors provides LightTokens) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            previewItems.forEachIndexed { i, item ->
                val p by animateFloatAsState(
                    if (entered) 1f else 0f,
                    tween(550, delayMillis = 200 + i * 120, easing = FastOutSlowInEasing),
                    label = "card$i",
                )
                val widthFraction = 1f - i * 0.07f
                Row(
                    Modifier
                        .fillMaxWidth(widthFraction)
                        .graphicsLayer {
                            alpha = p
                            translationY = (1f - p) * 36.dp.toPx()
                        }
                        .shadow(if (i == 0) 10.dp else 4.dp, RoundedCornerShape(18.dp), ambientColor = Color(0xFF0B2A6B), spotColor = Color(0xFF0B2A6B))
                        .clip(RoundedCornerShape(18.dp))
                        .background(previewCardColors[i])
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryIcon(item.category, 34.dp)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(item.title, color = Done.colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${item.time} · ${item.category}", color = Done.colors.muted, fontSize = 12.sp)
                    }
                    Box(
                        Modifier.size(24.dp).clip(CircleShape).background(Done.colors.accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

/** White status/navigation bar icons while this screen is showing (it sits on brand blue). */
@Composable
private fun LightSystemBarIcons() {
    val view = LocalView.current
    val dark = Done.colors.isDark
    DisposableEffect(view, dark) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            controller?.isAppearanceLightStatusBars = !dark
            controller?.isAppearanceLightNavigationBars = !dark
        }
    }
}
