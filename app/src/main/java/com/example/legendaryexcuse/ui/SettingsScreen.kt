package com.example.legendaryexcuse.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.legendaryexcuse.R
import com.example.legendaryexcuse.model.ModelDownloader
import com.example.legendaryexcuse.model.RoutingMode
import com.example.legendaryexcuse.model.Tone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(state: UiState, vm: ExcuseViewModel, onBack: () -> Unit) {
  BackHandler(onBack = onBack)
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.settings)) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) } },
      )
    },
  ) { padding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(stringResource(R.string.settings_default_tone), style = MaterialTheme.typography.titleMedium)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tone.entries.forEach { tone ->
          FilterChip(selected = state.settings.defaultTone == tone, onClick = { vm.setDefaultTone(tone) }, label = { Text(stringResource(tone.label())) })
        }
      }
      HorizontalDivider()

      Text(stringResource(R.string.settings_routing), style = MaterialTheme.typography.titleMedium)
      RoutingMode.entries.forEach { mode ->
        Row(
          Modifier.fillMaxWidth().selectable(selected = state.settings.routing == mode, onClick = { vm.setRouting(mode) }),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          RadioButton(selected = state.settings.routing == mode, onClick = { vm.setRouting(mode) })
          Text(stringResource(mode.label()))
        }
      }
      HorizontalDivider()

      Text(stringResource(R.string.settings_model), style = MaterialTheme.typography.titleMedium)
      Text(stringResource(R.string.settings_model_info, ModelDownloader.MODEL_NAME, (ModelDownloader.MODEL_SIZE_BYTES shr 20).toInt(), state.backend ?: "—"))
      OutlinedButton(onClick = { vm.deleteModel(); onBack() }, enabled = !state.isRunning) { Text(stringResource(R.string.settings_delete_model)) }
    }
  }
}
