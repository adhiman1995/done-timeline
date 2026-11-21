package com.donetimeline.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.donetimeline.app.DemoData
import com.donetimeline.app.DoneApp
import com.donetimeline.app.ui.theme.DoneTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels { MainViewModel.factory((application as DoneApp).container) }

    /** The launch splash stays until we know which screen to show (and, if signed in, tasks are loaded). */
    @Volatile private var ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { !ready }
        lifecycleScope.launch {
            // Cached tasks usually arrive within a few hundred ms; never hold the splash longer than 1.5 s.
            withTimeoutOrNull(1_500) { vm.state.first { it.account == null || it.loaded } }
            ready = true
        }

        enableEdgeToEdge()
        // Debug builds: `--ez demo true` previews the UI with sample data (DemoData is null in release).
        if (intent.getBooleanExtra("demo", false)) DemoData.tasks()?.let(vm::startDemo)
        setContent {
            DoneTheme {
                val state by vm.state.collectAsStateWithLifecycle()
                var showAccount by rememberSaveable { mutableStateOf(false) }
                when {
                    state.account == null -> SignInScreen(vm)
                    showAccount -> {
                        BackHandler { showAccount = false }
                        AccountScreen(vm, onBack = { showAccount = false })
                    }
                    else -> MainShell(vm, onOpenSettings = { showAccount = true })
                }
            }
        }
    }
}
