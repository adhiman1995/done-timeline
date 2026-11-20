package com.donetimeline.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.donetimeline.app.ui.theme.Done
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val c = Done.colors
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var url by rememberSaveable { mutableStateOf("") }
    var token by rememberSaveable { mutableStateOf("") }
    var initialised by rememberSaveable { mutableStateOf(false) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var result by rememberSaveable { mutableStateOf<String?>(null) }
    var resultOk by rememberSaveable { mutableStateOf(false) }

    // Prefill once from saved settings.
    LaunchedEffect(state.loaded) {
        if (state.loaded && !initialised) {
            url = state.settings.serverUrl
            token = state.settings.token
            initialised = true
        }
    }

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = c.text) }
            Text("Sync & server", color = c.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DoneCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel("Server")
                    OutlinedTextField(
                        value = url, onValueChange = { url = it; result = null },
                        label = { Text("Server URL") },
                        placeholder = { Text("https://your-api.onrender.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        shape = RoundedCornerShape(12.dp), colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = token, onValueChange = { token = it; result = null },
                        label = { Text("Access token") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp), colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Running the server on your computer? From the Android emulator use http://10.0.2.2:3001",
                        color = c.muted, fontSize = 12.5.sp,
                    )
                    result?.let { Text(it, color = if (resultOk) c.accentText else c.danger, fontSize = 13.5.sp) }
                    GradientButton(
                        text = if (busy) "Connecting…" else "Save & connect",
                        enabled = !busy && url.isNotBlank() && token.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            scope.launch {
                                busy = true
                                val error = vm.connect(url, token)
                                resultOk = error == null
                                result = error ?: "Connected. Your tasks are syncing."
                                busy = false
                            }
                        },
                    )
                }
            }

            DoneCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Sync")
                    InfoRow("Status", when {
                        !state.settings.isConfigured -> "Not connected"
                        state.syncing -> "Syncing…"
                        else -> "Connected"
                    })
                    InfoRow("Last synced", if (state.settings.lastSyncAt > 0) relativeTime(state.settings.lastSyncAt) else "Never")
                    InfoRow("Waiting to sync", "${state.pending}")
                    Spacer(Modifier.height(2.dp))
                    GradientButton(
                        text = if (state.syncing) "Syncing…" else "Sync now",
                        enabled = state.settings.isConfigured && !state.syncing,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { vm.syncNow() },
                    )
                }
            }

            DoneCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel("How sync works")
                    Text(
                        "Everything you log is saved on this phone first, so the app works offline. " +
                            "Changes are sent to your server as soon as you're online, and edits made on the " +
                            "website are pulled in when you open the app and about every 15 minutes. " +
                            "If the same task is edited in two places, the most recent edit wins.",
                        color = c.text2, fontSize = 13.5.sp, lineHeight = 20.sp,
                    )
                }
            }
        }
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
