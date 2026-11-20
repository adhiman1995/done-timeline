package com.donetimeline.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.donetimeline.app.ui.theme.Done

@Composable
fun AccountScreen(vm: MainViewModel, onBack: () -> Unit) {
    val c = Done.colors
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val account = state.account

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = c.text) }
            Text("Account & sync", color = c.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DoneCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(account?.name?.ifBlank { null } ?: account?.email.orEmpty())
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(account?.name?.ifBlank { null } ?: "Signed in", color = c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(account?.email.orEmpty(), color = c.text2, fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            DoneCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Sync")
                    InfoRow("Status", when {
                        !state.loaded -> "Loading…"
                        state.offline && state.pendingWrites -> "Offline · will sync later"
                        state.pendingWrites -> "Syncing…"
                        state.offline -> "Offline"
                        else -> "Up to date"
                    })
                    InfoRow("Tasks", "${state.totalTasks}")
                }
            }

            DoneCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel("How sync works")
                    Text(
                        "Everything you log is saved on this phone first, so the app works offline. " +
                            "Changes upload as soon as you're online, and edits made on the website appear here " +
                            "within seconds while the app is open.",
                        color = c.text2, fontSize = 13.5.sp, lineHeight = 20.sp,
                    )
                }
            }

            OutlinedButton(
                onClick = { vm.signOut(context); onBack() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = CircleShape,
                border = BorderStroke(1.dp, c.border),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = c.danger),
            ) {
                Text("Sign out", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun Avatar(name: String) {
    val c = Done.colors
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(c.field).border(1.dp, c.border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(name.firstOrNull()?.uppercase() ?: "?", color = c.accentText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val c = Done.colors
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = c.text2, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
