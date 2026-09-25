package com.example.legendaryexcuse.model

/** Which "brain" runs a step: the on-device model, the cloud model, or plain deterministic code. */
enum class Brain { LOCAL, CLOUD, CODE }

enum class Severity { MINOR, MODERATE, SEVERE }

enum class ExcuseContext { WORK, SCHOOL, PARTNER, FRIENDS, FAMILY, OTHER }

enum class Tone { AUTO, FORMAL, DRAMATIC, ABSURD }

enum class StepStatus { PENDING, RUNNING, DONE, SKIPPED, ERROR }

enum class RoutingMode { AUTO, FORCE_LOCAL, FORCE_CLOUD }

enum class StepId { ANONYMIZE, LEAK_CHECK, SUMMARIZE, SEVERITY, STYLE, PROMPT, GENERATE, REVIEW, DEANONYMIZE }

enum class NoteType { FIXED_BY_CODE, SKIPPED_SHORT_INPUT, RECOVERED_ERROR, NOT_NEEDED, CLOUD_FALLBACK }

/** Why the result card shows an extra warning. */
enum class Notice { ANONYMIZATION_FAILED, OFFLINE_FALLBACK, CLOUD_ERROR_FALLBACK }

data class PipelineStep(
  val id: StepId,
  val brain: Brain,
  val status: StepStatus = StepStatus.PENDING,
  val durationMs: Long? = null,
  val output: String? = null, // raw JSON or text produced by the step
  val note: NoteType? = null,
)

data class Anonymization(val text: String, val map: Map<String, String>)

data class Analysis(
  val summary: String,
  val severity: Severity,
  val severityReason: String,
  val context: ExcuseContext,
  val tone: Tone,
  val approach: String,
  val theme: String, // picked at random by code so the same story never gets the same excuse
)

data class Review(val approved: Boolean, val reason: String)

data class ExcuseResult(
  val excuse: String, // already de-anonymized
  val generatedBy: Brain, // LOCAL or CLOUD
  val textSentToCloud: String?, // null if nothing was sent
  val reviewerWarning: Boolean,
  val notice: Notice? = null,
  val noticeDetail: String? = null, // technical cause, e.g. an exception message
)

sealed interface ModelState {
  data object NotDownloaded : ModelState
  data class Downloading(val downloadedBytes: Long, val totalBytes: Long) : ModelState
  data object Initializing : ModelState
  data class Ready(val backend: String) : ModelState
  data class Failed(val error: String) : ModelState
  data class NotEnoughSpace(val missingBytes: Long) : ModelState
}

/** The steps before anything runs: every step pending, with the brain it will (probably) use. */
fun initialSteps(): List<PipelineStep> = StepId.entries.map { id ->
  val brain = when (id) {
    StepId.LEAK_CHECK, StepId.DEANONYMIZE -> Brain.CODE
    else -> Brain.LOCAL
  }
  PipelineStep(id, brain)
}
