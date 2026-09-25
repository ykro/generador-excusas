package com.example.legendaryexcuse.pipeline

import com.example.legendaryexcuse.brains.CloudBrain
import com.example.legendaryexcuse.brains.LocalBrain
import com.example.legendaryexcuse.model.Analysis
import com.example.legendaryexcuse.model.Anonymization
import com.example.legendaryexcuse.model.Brain
import com.example.legendaryexcuse.model.ExcuseContext
import com.example.legendaryexcuse.model.ExcuseResult
import com.example.legendaryexcuse.model.NoteType
import com.example.legendaryexcuse.model.Notice
import com.example.legendaryexcuse.model.PipelineStep
import com.example.legendaryexcuse.model.Review
import com.example.legendaryexcuse.model.RoutingMode
import com.example.legendaryexcuse.model.Severity
import com.example.legendaryexcuse.model.StepId
import com.example.legendaryexcuse.model.StepStatus
import com.example.legendaryexcuse.model.Tone
import java.io.IOException
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException

/**
 * The split brain, in plain Kotlin. Every step says which brain runs it:
 *
 *   LOCAL  (Gemma, on the phone): anonymize, summarize, severity, style, approach, review
 *   CODE   (deterministic):       leak check, de-anonymize
 *   CLOUD  (Gemini):              generate the excuse, only when it is worth it
 *
 * The orchestration (order, routing, retries) is deliberately NOT an agent: it has to be explicit
 * and predictable, because it decides what is allowed to leave the phone.
 */
class ExcusePipeline(
  private val local: LocalBrain,
  private val cloud: CloudBrain,
  private val prompts: PromptLoader,
  private val deanonymizer: Deanonymizer,
  private val random: Random = Random.Default,
) {
  private var lastTheme: String? = null

  suspend fun run(
    story: String,
    userTone: Tone,
    routing: RoutingMode,
    isOnline: Boolean,
    onStep: (PipelineStep) -> Unit,
  ): ExcuseResult {
    // 1. Anonymize (LOCAL): the real story never leaves the phone.
    val anonymized = step(StepId.ANONYMIZE, Brain.LOCAL, onStep) {
      val json = askJson<AnonymizeJson>(StepId.ANONYMIZE, prompts.fill("anonymize", mapOf("story" to story)))
      if (json == null) StepOutcome(null, "{}", NoteType.RECOVERED_ERROR, StepStatus.ERROR)
      else StepOutcome(Anonymization(json.text, json.map), JsonParsing.json.encodeToString(AnonymizeJson.serializer(), json))
    }
    val anonymizationFailed = anonymized == null

    // Leak check (CODE): never trust the small model blindly.
    val safe = step(StepId.LEAK_CHECK, Brain.CODE, onStep) {
      if (anonymized == null) return@step StepOutcome(Anonymization(story, emptyMap()), status = StepStatus.SKIPPED)
      val result = LeakDetector.check(anonymized)
      StepOutcome(result.anonymization, result.anonymization.text, if (result.fixed) NoteType.FIXED_BY_CODE else null)
    }

    // 2. Summarize (LOCAL): fewer tokens = cheaper and less exposure. Skipped for short stories.
    val summary = step(StepId.SUMMARIZE, Brain.LOCAL, onStep) {
      if (safe.text.wordCount() < SUMMARY_MAX_WORDS) {
        return@step StepOutcome(safe.text, safe.text, NoteType.SKIPPED_SHORT_INPUT, StepStatus.SKIPPED)
      }
      val json = askJson<SummaryJson>(StepId.SUMMARIZE, prompts.fill("summarize", mapOf("anonymized_text" to safe.text)))
      StepOutcome(json?.summary ?: safe.text, json?.summary ?: safe.text, if (json == null) NoteType.RECOVERED_ERROR else null)
    }

    // 3. Severity (LOCAL): decides whether paying for the API is worth it.
    val severity = step(StepId.SEVERITY, Brain.LOCAL, onStep) {
      val json = askJson<SeverityJson>(StepId.SEVERITY, prompts.fill("severity", mapOf("summary" to summary)))
      val parsed = json?.let { JsonParsing.enumOrNull<Severity>(it.severity) }
      val value = parsed ?: Severity.MODERATE
      val reason = json?.reason ?: ""
      StepOutcome(value to reason, "${value.name.lowercase()} — $reason", if (parsed == null) NoteType.RECOVERED_ERROR else null)
    }

    // 4. Style (LOCAL): a simple classification, perfect for a small model. A manual tone wins.
    val style = step(StepId.STYLE, Brain.LOCAL, onStep) {
      val json = askJson<StyleJson>(StepId.STYLE, prompts.fill("style", mapOf("summary" to summary)))
      val context = json?.let { JsonParsing.enumOrNull<ExcuseContext>(it.context) } ?: ExcuseContext.OTHER
      val tone = if (userTone != Tone.AUTO) userTone else json?.let { JsonParsing.enumOrNull<Tone>(it.tone) }?.takeIf { it != Tone.AUTO } ?: Tone.DRAMATIC
      StepOutcome(context to tone, "context=${context.name.lowercase()}, tone=${tone.name.lowercase()}", if (json == null) NoteType.RECOVERED_ERROR else null)
    }

    // 5. Prompt building (LOCAL + CODE): code picks a random theme, never the previous one (so every run is different),
    // the model suggests an approach, and code fills the base template.
    val analysis = step(StepId.PROMPT, Brain.LOCAL, onStep) {
      val theme = prompts.lines("themes").filter { it != lastTheme }.random(random).also { lastTheme = it }
      val vars = mapOf("summary" to summary, "context" to style.first.name.lowercase(), "severity" to severity.first.name.lowercase(), "theme" to theme)
      val json = askJson<ApproachJson>(StepId.PROMPT, prompts.fill("approach", vars))
      val analysis = Analysis(summary, severity.first, severity.second, style.first, style.second, json?.approach ?: "", theme)
      StepOutcome(analysis, buildPrompt(analysis, correction = null), if (json == null) NoteType.RECOVERED_ERROR else null)
    }
    val prompt = buildPrompt(analysis, correction = null)

    // 6. Routing: THIS is the split-brain decision.
    val wantsCloud = when (routing) {
      RoutingMode.FORCE_LOCAL -> false
      RoutingMode.FORCE_CLOUD -> true
      RoutingMode.AUTO -> analysis.severity != Severity.MINOR
    }
    // Never send anything to the cloud if anonymization failed, and don't try without internet.
    val useCloud = wantsCloud && isOnline && !anonymizationFailed
    val offlineNotice = if (wantsCloud && !isOnline) Notice.OFFLINE_FALLBACK else null
    val baseNotice = if (anonymizationFailed) Notice.ANONYMIZATION_FAILED else offlineNotice

    if (useCloud) {
      val sent = mutableListOf<String>() // everything that left the phone, so tests can assert on it
      val cloudResult = runCatchingCloud { generateInCloud(analysis, prompt, sent, onStep) }
      if (cloudResult.isSuccess) {
        val (excuse, warning) = cloudResult.getOrThrow()
        return finish(excuse, Brain.CLOUD, sent.joinToString(PROMPT_SEPARATOR), warning, safe.map, null, null, onStep)
      }
      // Network error, timeout, App Check or quota: fall back to the local brain.
      val error = cloudResult.exceptionOrNull()
      val notice = if (error is IOException || error is TimeoutCancellationException) Notice.OFFLINE_FALLBACK else Notice.CLOUD_ERROR_FALLBACK
      val excuse = generateLocally(prompt, onStep, NoteType.CLOUD_FALLBACK)
      skip(StepId.REVIEW, onStep)
      val sentText = sent.takeIf { it.isNotEmpty() }?.joinToString(PROMPT_SEPARATOR)
      return finish(excuse, Brain.LOCAL, sentText, false, safe.map, notice, error?.message, onStep)
    }

    // 6a. Generate (LOCAL): free and works offline. No review needed.
    val excuse = generateLocally(prompt, onStep, note = null)
    skip(StepId.REVIEW, onStep)
    return finish(excuse, Brain.LOCAL, null, false, safe.map, baseNotice, null, onStep)
  }

  /** 6b + 7. Gemini writes, Gemma reviews; one retry with the reviewer's reason. */
  private suspend fun generateInCloud(
    analysis: Analysis, prompt: String, sent: MutableList<String>, onStep: (PipelineStep) -> Unit,
  ): Pair<String, Boolean> {
    sent += prompt
    var excuse = step(StepId.GENERATE, Brain.CLOUD, onStep) { cloud.generate(prompt).trim().let { StepOutcome(it, it) } }
    var review = reviewLocally(analysis, excuse, onStep)
    if (!review.approved) {
      val retryPrompt = buildPrompt(analysis, correction = review.reason)
      sent += retryPrompt
      excuse = step(StepId.GENERATE, Brain.CLOUD, onStep) { cloud.generate(retryPrompt).trim().let { StepOutcome(it, it) } }
      review = reviewLocally(analysis, excuse, onStep)
    }
    return excuse to !review.approved
  }

  private suspend fun reviewLocally(analysis: Analysis, excuse: String, onStep: (PipelineStep) -> Unit): Review =
    step(StepId.REVIEW, Brain.LOCAL, onStep) {
      // Small models can't count words reliably, so the length rule is checked by code.
      if (excuse.wordCount() > REVIEW_MAX_WORDS) {
        return@step Review(false, "The excuse is longer than $REVIEW_MAX_WORDS words.").let { StepOutcome(it, it.reason) }
      }
      val vars = mapOf("summary" to analysis.summary, "context" to analysis.context.name.lowercase(), "excuse" to excuse)
      val json = askJson<ReviewJson>(StepId.REVIEW, prompts.fill("review", vars))
      // If the reviewer can't answer in JSON we accept the excuse rather than loop.
      val review = Review(json?.approved ?: true, json?.reason ?: "")
      StepOutcome(review, "approved=${review.approved} — ${review.reason}", if (json == null) NoteType.RECOVERED_ERROR else null)
    }

  private suspend fun generateLocally(prompt: String, onStep: (PipelineStep) -> Unit, note: NoteType?): String {
    val start = System.currentTimeMillis()
    val text = StringBuilder()
    onStep(PipelineStep(StepId.GENERATE, Brain.LOCAL, StepStatus.RUNNING, note = note))
    local.stream(prompt).collect { chunk ->
      text.append(chunk)
      onStep(PipelineStep(StepId.GENERATE, Brain.LOCAL, StepStatus.RUNNING, output = text.toString(), note = note))
    }
    val excuse = text.toString().trim()
    onStep(PipelineStep(StepId.GENERATE, Brain.LOCAL, StepStatus.DONE, System.currentTimeMillis() - start, excuse, note))
    return excuse
  }

  // 8. De-anonymize (CODE): exact replacement, no hallucinations.
  private suspend fun finish(
    excuse: String, generatedBy: Brain, sent: String?, warning: Boolean, map: Map<String, String>,
    notice: Notice?, detail: String?, onStep: (PipelineStep) -> Unit,
  ): ExcuseResult {
    val final = step(StepId.DEANONYMIZE, Brain.CODE, onStep) { deanonymizer.restore(excuse, map).let { StepOutcome(it, it) } }
    return ExcuseResult(final, generatedBy, sent, warning, notice, detail)
  }

  private fun buildPrompt(analysis: Analysis, correction: String?): String = prompts.fill(
    "base_template",
    mapOf(
      "summary" to analysis.summary,
      "context" to analysis.context.name.lowercase(),
      "severity" to analysis.severity.name.lowercase(),
      "tone" to analysis.tone.name.lowercase(),
      "approach" to analysis.approach,
      "theme" to analysis.theme,
      "correction" to (correction?.let { prompts.fill("correction", mapOf("reason" to it)) } ?: ""),
    ),
  )

  /** Asks the local model for JSON; on invalid JSON retries once, then returns null (caller uses defaults). */
  private suspend inline fun <reified T> askJson(step: StepId, prompt: String): T? {
    JsonParsing.parse<T>(local.runJson(step, prompt))?.let { return it }
    return JsonParsing.parse<T>(local.runJson(step, "$prompt\nReply ONLY with valid JSON."))
  }

  private class StepOutcome<T>(val value: T, val output: String? = null, val note: NoteType? = null, val status: StepStatus = StepStatus.DONE)

  /** Runs one step, reporting RUNNING → DONE/SKIPPED/ERROR with its duration. */
  private suspend fun <T> step(id: StepId, brain: Brain, onStep: (PipelineStep) -> Unit, block: suspend () -> StepOutcome<T>): T {
    val start = System.currentTimeMillis()
    onStep(PipelineStep(id, brain, StepStatus.RUNNING))
    try {
      val outcome = block()
      onStep(PipelineStep(id, brain, outcome.status, System.currentTimeMillis() - start, outcome.output, outcome.note))
      return outcome.value
    } catch (e: Exception) {
      if (e !is CancellationException || e is TimeoutCancellationException) {
        onStep(PipelineStep(id, brain, StepStatus.ERROR, System.currentTimeMillis() - start, e.message))
      }
      throw e
    }
  }

  private fun skip(id: StepId, onStep: (PipelineStep) -> Unit) =
    onStep(PipelineStep(id, Brain.LOCAL, StepStatus.SKIPPED, note = NoteType.NOT_NEEDED))

  /** Like runCatching, but lets real cancellation (user pressed cancel) propagate. */
  private suspend fun <T> runCatchingCloud(block: suspend () -> T): Result<T> = try {
    Result.success(block())
  } catch (e: TimeoutCancellationException) {
    Result.failure(e)
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    Result.failure(e)
  }

  private fun String.wordCount() = trim().split(Regex("""\s+""")).count { it.isNotEmpty() }

  companion object {
    const val SUMMARY_MAX_WORDS = 60
    const val REVIEW_MAX_WORDS = 120
    const val PROMPT_SEPARATOR = "\n\n---\n\n"
  }
}
