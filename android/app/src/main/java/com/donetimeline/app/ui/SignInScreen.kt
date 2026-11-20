package com.donetimeline.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.donetimeline.app.ui.theme.Done
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(vm: MainViewModel) {
    val c = Done.colors
    val context = LocalContext.current // the Activity: Credential Manager needs it to show the account picker
    val scope = rememberCoroutineScope()
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    Box(
        Modifier.fillMaxSize().background(c.bg).statusBarsPadding().navigationBarsPadding().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            AppLogo(size = 64.dp)
            Spacer(Modifier.height(20.dp))
            Text("Done Timeline", color = c.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Log what you finish and see it on a timeline. Works offline and syncs with the website.",
                color = c.text2, fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            GradientButton(
                text = if (busy) "Signing in…" else "Continue with Google",
                enabled = !busy,
                height = 52.dp,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        busy = true
                        error = null
                        error = vm.signIn(context)
                        busy = false
                    }
                },
            )
            error?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = c.danger, fontSize = 13.5.sp, textAlign = TextAlign.Center)
            }
        }
    }
}
