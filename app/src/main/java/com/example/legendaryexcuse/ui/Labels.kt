package com.example.legendaryexcuse.ui

import androidx.annotation.StringRes
import com.example.legendaryexcuse.R
import com.example.legendaryexcuse.model.Brain
import com.example.legendaryexcuse.model.RoutingMode
import com.example.legendaryexcuse.model.PipelineStep
import com.example.legendaryexcuse.model.StepId
import com.example.legendaryexcuse.model.Tone

/** The domain never contains Spanish text: enums are mapped to strings.xml here. */

@StringRes fun Tone.label() = when (this) {
  Tone.AUTO -> R.string.tone_auto
  Tone.FORMAL -> R.string.tone_formal
  Tone.DRAMATIC -> R.string.tone_dramatic
  Tone.ABSURD -> R.string.tone_absurd
}

@StringRes fun RoutingMode.label() = when (this) {
  RoutingMode.AUTO -> R.string.routing_auto
  RoutingMode.FORCE_LOCAL -> R.string.routing_force_local
  RoutingMode.FORCE_CLOUD -> R.string.routing_force_cloud
}

@StringRes fun Tone.shortLabel() = when (this) {
  Tone.AUTO -> R.string.tone_short_auto
  Tone.FORMAL -> R.string.tone_short_formal
  Tone.DRAMATIC -> R.string.tone_short_dramatic
  Tone.ABSURD -> R.string.tone_short_absurd
}

/** Friendly message for the step that is running, naming the brain that works on it. */
@StringRes fun PipelineStep.progressMessage() = when (id) {
  StepId.ANONYMIZE -> R.string.progress_anonymize
  StepId.LEAK_CHECK -> R.string.progress_leak_check
  StepId.SUMMARIZE -> R.string.progress_summarize
  StepId.SEVERITY -> R.string.progress_severity
  StepId.STYLE -> R.string.progress_style
  StepId.PROMPT -> R.string.progress_prompt
  StepId.GENERATE -> if (brain == Brain.CLOUD) R.string.progress_generate_cloud else R.string.progress_generate_local
  StepId.REVIEW -> R.string.progress_review
  StepId.DEANONYMIZE -> R.string.progress_deanonymize
}
