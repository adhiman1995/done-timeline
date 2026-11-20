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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.donetimeline.app.DemoData
import com.donetimeline.app.DoneApp
import com.donetimeline.app.ui.theme.DoneTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels { MainViewModel.factory((application as DoneApp).container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    else -> TimelineScreen(vm, onOpenSettings = { showAccount = true })
                }
            }
        }
    }
}
