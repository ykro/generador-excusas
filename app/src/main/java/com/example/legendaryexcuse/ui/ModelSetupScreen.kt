package com.example.legendaryexcuse.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.legendaryexcuse.R
import com.example.legendaryexcuse.model.ModelState

@Composable
fun ModelSetupScreen(model: ModelState, onDownload: () -> Unit, onCancel: () -> Unit) {
  Column(
    modifier = Modifier.fillMaxSize().padding(32.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    when (model) {
      is ModelState.Downloading -> {
        Text(stringResource(R.string.setup_downloading), textAlign = TextAlign.Center)
        LinearProgressIndicator(progress = { model.downloadedBytes.toFloat() / model.totalBytes }, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.setup_progress, (model.downloadedBytes shr 20).toInt(), (model.totalBytes shr 20).toInt()))
        OutlinedButton(onClick = onCancel) { Text(stringResource(R.string.setup_cancel)) }
      }
      ModelState.NotDownloaded -> {
        Text(stringResource(R.string.setup_not_downloaded), textAlign = TextAlign.Center)
        Button(onClick = onDownload) { Text(stringResource(R.string.setup_download)) }
      }
      ModelState.Initializing, is ModelState.Ready -> {
        CircularProgressIndicator()
        Text(stringResource(R.string.setup_initializing))
      }
      is ModelState.Failed -> {
        Text(stringResource(R.string.setup_failed, model.error), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
        Button(onClick = onDownload) { Text(stringResource(R.string.setup_retry)) }
      }
      is ModelState.NotEnoughSpace -> {
        Text(stringResource(R.string.setup_no_space, (model.missingBytes shr 20).toInt()), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
        Button(onClick = onDownload) { Text(stringResource(R.string.setup_retry)) }
      }
    }
  }
}
