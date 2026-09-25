package com.example.legendaryexcuse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.legendaryexcuse.model.ModelState
import com.example.legendaryexcuse.theme.ExcusaLegendariaTheme
import com.example.legendaryexcuse.ui.ExcuseViewModel
import com.example.legendaryexcuse.ui.MainScreen
import com.example.legendaryexcuse.ui.ModelSetupScreen
import com.example.legendaryexcuse.ui.SettingsScreen

/** Single activity. Navigation is just a `when`: setup until the local model is ready, then main ↔ settings. */
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      ExcusaLegendariaTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          val vm: ExcuseViewModel = viewModel()
          val state by vm.state.collectAsStateWithLifecycle()
          var showSettings by rememberSaveable { mutableStateOf(false) }
          when {
            state.model !is ModelState.Ready -> ModelSetupScreen(state.model, onDownload = vm::prepareModel, onCancel = vm::cancelDownload)
            showSettings -> SettingsScreen(state, vm, onBack = { showSettings = false })
            else -> MainScreen(state, vm, onOpenSettings = { showSettings = true })
          }
        }
      }
    }
  }
}
