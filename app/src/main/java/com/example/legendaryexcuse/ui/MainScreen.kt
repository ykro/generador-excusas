package com.example.legendaryexcuse.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.legendaryexcuse.R
import com.example.legendaryexcuse.model.Brain
import com.example.legendaryexcuse.model.ExcuseResult
import com.example.legendaryexcuse.model.Notice
import com.example.legendaryexcuse.model.PipelineStep
import com.example.legendaryexcuse.model.StepId
import com.example.legendaryexcuse.model.StepStatus
import com.example.legendaryexcuse.model.Tone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(state: UiState, vm: ExcuseViewModel, onOpenSettings: () -> Unit) {
  val listState = rememberLazyListState()
  // When the excuse arrives, bring it into view.
  LaunchedEffect(state.result) {
    if (state.result != null) listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
  }
  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        },
        actions = {
          ConnectionPill(state.isOnline)
          IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings)) }
        },
      )
    },
  ) { padding ->
    LazyColumn(
      state = listState,
      modifier = Modifier.fillMaxSize().padding(padding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item { StoryCard(state, vm) }
      item {
        if (state.isRunning) {
          ProgressCard(state.steps, onCancel = vm::cancel)
        } else {
          Button(
            onClick = vm::generate,
            enabled = state.input.isNotBlank(),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
          ) { Text(stringResource(R.string.generate), style = MaterialTheme.typography.titleMedium) }
        }
      }
      state.error?.let { item { Text(stringResource(R.string.error_generic, it), color = MaterialTheme.colorScheme.error) } }
      state.result?.let { result ->
        item { ResultCard(result, onAnother = vm::generate) }
      }
    }
  }
}

@Composable
private fun ConnectionPill(isOnline: Boolean) {
  val color = if (isOnline) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
  Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.12f)) {
    Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
      Surface(shape = RoundedCornerShape(50), color = color, modifier = Modifier.size(8.dp)) {}
      Spacer(Modifier.width(6.dp))
      Text(stringResource(if (isOnline) R.string.online else R.string.offline), color = color, style = MaterialTheme.typography.labelMedium)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoryCard(state: UiState, vm: ExcuseViewModel) {
  val examples = listOf(
    R.string.example_minor_label to R.string.example_minor,
    R.string.example_moderate_label to R.string.example_moderate,
    R.string.example_severe_label to R.string.example_severe,
  )
  val enabled = !state.isRunning
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    OutlinedTextField(
      value = state.input,
      onValueChange = vm::onInputChange,
      placeholder = { Text(stringResource(R.string.input_placeholder)) },
      enabled = enabled,
      shape = RoundedCornerShape(16.dp),
      textStyle = MaterialTheme.typography.bodyLarge,
      modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(stringResource(R.string.examples_title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        examples.forEach { (label, story) ->
          val text = stringResource(story)
          SuggestionChip(onClick = { vm.onInputChange(text) }, label = { Text(stringResource(label)) }, enabled = enabled)
        }
      }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(stringResource(R.string.tone_title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
      SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        Tone.entries.forEachIndexed { index, tone ->
          SegmentedButton(
            selected = state.tone == tone,
            onClick = { vm.onToneChange(tone) },
            shape = SegmentedButtonDefaults.itemShape(index, Tone.entries.size),
            enabled = enabled,
            icon = {},
          ) { Text(stringResource(tone.shortLabel()), maxLines = 1) }
        }
      }
    }
  }
}

/** While the pipeline runs: which brain is working, how far along we are, and the excuse as it is written. */
@Composable
private fun ProgressCard(steps: List<PipelineStep>, onCancel: () -> Unit) {
  val current = steps.lastOrNull { it.status == StepStatus.RUNNING }
  val finished = steps.count { it.status == StepStatus.DONE || it.status == StepStatus.SKIPPED }
  Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(12.dp))
        Text(current?.let { stringResource(it.progressMessage()) } ?: "", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
      }
      LinearProgressIndicator(progress = { finished.toFloat() / steps.size.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth())
      // Local generation streams: show the text as Gemma writes it.
      current?.takeIf { it.id == StepId.GENERATE }?.output?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
      TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.cancel)) }
    }
  }
}

@Composable
private fun ResultCard(result: ExcuseResult, onAnother: () -> Unit) {
  val context = LocalContext.current
  val copiedMessage = stringResource(R.string.copied)
  Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
      Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)) {
        Text(
          stringResource(if (result.generatedBy == Brain.CLOUD) R.string.generated_by_cloud else R.string.generated_by_local),
          style = MaterialTheme.typography.labelMedium,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
      }
      Text(result.excuse, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 28.sp))
      val warning = when (result.notice) {
        Notice.ANONYMIZATION_FAILED -> stringResource(R.string.notice_anonymization_failed)
        Notice.OFFLINE_FALLBACK -> stringResource(R.string.notice_offline)
        Notice.CLOUD_ERROR_FALLBACK -> stringResource(R.string.notice_cloud_error, result.noticeDetail ?: "?")
        null -> null
      }
      warning?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
      if (result.reviewerWarning) Text(stringResource(R.string.reviewer_warning), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {
          context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("excuse", result.excuse))
          Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
        }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.copy)) }
        OutlinedButton(onClick = {
          val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, result.excuse)
          context.startActivity(Intent.createChooser(send, null))
        }, modifier = Modifier.weight(1f)) {
          Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(stringResource(R.string.share))
        }
      }
      Button(
        onClick = onAnother,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary),
        modifier = Modifier.fillMaxWidth(),
      ) { Text(stringResource(R.string.another)) }
    }
  }
}
