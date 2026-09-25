package com.example.legendaryexcuse.pipeline

import com.example.legendaryexcuse.model.Brain
import com.example.legendaryexcuse.model.ExcuseResult
import com.example.legendaryexcuse.model.NoteType
import com.example.legendaryexcuse.model.Notice
import com.example.legendaryexcuse.model.PipelineStep
import com.example.legendaryexcuse.model.RoutingMode
import com.example.legendaryexcuse.model.StepId
import com.example.legendaryexcuse.model.StepStatus
import com.example.legendaryexcuse.model.Tone
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExcusePipelineTest {
  private val steps = mutableMapOf<StepId, PipelineStep>()

  private suspend fun run(
    local: FakeLocalBrain,
    cloud: FakeCloudBrain = FakeCloudBrain(),
    story: String = LAURA_STORY,
    online: Boolean = true,
    routing: RoutingMode = RoutingMode.AUTO,
    tone: Tone = Tone.AUTO,
  ): ExcuseResult =
    ExcusePipeline(local, cloud, realPrompts, Deanonymizer(testGenerics)).run(story, tone, routing, online) { steps[it.id] = it }

  @Test fun severeStoryGoesToGeminiWithoutRealNames() = runTest {
    val cloud = FakeCloudBrain()
    val result = run(FakeLocalBrain(happyResponses("severe")), cloud)

    assertEquals(Brain.CLOUD, result.generatedBy)
    val sent = result.textSentToCloud!!
    assertFalse(sent.contains("Laura"))
    assertFalse(sent.contains("Oficinas Norte"))
    assertEquals("Perdón Laura, un dragón bloqueó Oficinas Norte.", result.excuse)
    assertEquals(StepStatus.DONE, steps.getValue(StepId.REVIEW).status)
    assertEquals(1, cloud.prompts.size)
  }

  @Test fun minorStoryStaysOnThePhone() = runTest {
    val local = FakeLocalBrain(happyResponses("minor"))
    val cloud = FakeCloudBrain()
    val result = run(local, cloud)

    assertEquals(Brain.LOCAL, result.generatedBy)
    assertNull(result.textSentToCloud)
    assertTrue(cloud.prompts.isEmpty())
    assertEquals("Lo siento Laura, hubo un eclipse.", result.excuse)
    assertEquals(StepStatus.SKIPPED, steps.getValue(StepId.REVIEW).status)
  }

  @Test fun offlineGeneratesLocally() = runTest {
    val cloud = FakeCloudBrain()
    val result = run(FakeLocalBrain(happyResponses("severe")), cloud, online = false)
    assertEquals(Brain.LOCAL, result.generatedBy)
    assertEquals(Notice.OFFLINE_FALLBACK, result.notice)
    assertTrue(cloud.prompts.isEmpty())
  }

  @Test fun networkErrorFallsBackToLocal() = runTest {
    val result = run(FakeLocalBrain(happyResponses("severe")), FakeCloudBrain(error = IOException("no route")))
    assertEquals(Brain.LOCAL, result.generatedBy)
    assertEquals(Notice.OFFLINE_FALLBACK, result.notice)
    assertEquals(NoteType.CLOUD_FALLBACK, steps.getValue(StepId.GENERATE).note)
  }

  @Test fun otherCloudErrorsAreReportedWithCause() = runTest {
    val result = run(FakeLocalBrain(happyResponses("severe")), FakeCloudBrain(error = IllegalStateException("App Check failed")))
    assertEquals(Notice.CLOUD_ERROR_FALLBACK, result.notice)
    assertEquals("App Check failed", result.noticeDetail)
  }

  @Test fun rejectedExcuseIsRetriedOnceWithReason() = runTest {
    val responses = happyResponses("severe") + (StepId.REVIEW to listOf("""{"approved": false, "reason": "too long"}""", """{"approved": true, "reason": "ok"}"""))
    val cloud = FakeCloudBrain(listOf("v1", "v2"))
    val result = run(FakeLocalBrain(responses), cloud)

    assertEquals(2, cloud.prompts.size)
    assertTrue(cloud.prompts[1].contains("because: too long"))
    assertEquals("v2", result.excuse)
    assertFalse(result.reviewerWarning)
  }

  @Test fun twoRejectionsShowWarning() = runTest {
    val responses = happyResponses("severe") + (StepId.REVIEW to listOf("""{"approved": false, "reason": "bad"}"""))
    val cloud = FakeCloudBrain(listOf("v1", "v2"))
    val result = run(FakeLocalBrain(responses), cloud)
    assertEquals(2, cloud.prompts.size)
    assertTrue(result.reviewerWarning)
    assertEquals("v2", result.excuse)
  }

  @Test fun failedAnonymizationNeverReachesTheCloud() = runTest {
    val responses = happyResponses("severe") + (StepId.ANONYMIZE to listOf("I can't do that"))
    val cloud = FakeCloudBrain()
    val result = run(FakeLocalBrain(responses), cloud, routing = RoutingMode.FORCE_CLOUD)

    assertTrue(cloud.prompts.isEmpty())
    assertEquals(Brain.LOCAL, result.generatedBy)
    assertEquals(Notice.ANONYMIZATION_FAILED, result.notice)
    assertEquals(StepStatus.ERROR, steps.getValue(StepId.ANONYMIZE).status)
  }

  @Test fun leakIsFixedByCodeBeforeLeavingThePhone() = runTest {
    val leaky = """{"text": "No llegué a la junta con mi jefa Laura en [PLACE_1]", "map": {"[PERSON_1]": "Laura", "[PLACE_1]": "Oficinas Norte"}}"""
    val responses = happyResponses("severe") + (StepId.ANONYMIZE to listOf(leaky))
    val cloud = FakeCloudBrain()
    val result = run(FakeLocalBrain(responses), cloud)

    assertEquals(NoteType.FIXED_BY_CODE, steps.getValue(StepId.LEAK_CHECK).note)
    assertFalse(result.textSentToCloud!!.contains("Laura"))
  }

  @Test fun invalidJsonIsRetriedThenDefaulted() = runTest {
    val responses = happyResponses("severe") + (StepId.SEVERITY to listOf("nope", "still nope"))
    val local = FakeLocalBrain(responses)
    val result = run(local)
    assertEquals(2, local.calls.count { it.first == StepId.SEVERITY })
    assertEquals(NoteType.RECOVERED_ERROR, steps.getValue(StepId.SEVERITY).note)
    assertEquals(Brain.CLOUD, result.generatedBy) // default severity is moderate
  }

  @Test fun forceLocalNeverCallsCloud() = runTest {
    val cloud = FakeCloudBrain()
    run(FakeLocalBrain(happyResponses("severe")), cloud, routing = RoutingMode.FORCE_LOCAL)
    assertTrue(cloud.prompts.isEmpty())
  }

  @Test fun manualToneWins() = runTest {
    val cloud = FakeCloudBrain()
    run(FakeLocalBrain(happyResponses("severe")), cloud, tone = Tone.ABSURD)
    assertTrue(cloud.prompts.single().contains("Tone: absurd"))
  }

  @Test fun shortStorySkipsSummary() = runTest {
    run(FakeLocalBrain(happyResponses("minor")))
    assertEquals(StepStatus.SKIPPED, steps.getValue(StepId.SUMMARIZE).status)
  }

  @Test fun tooLongExcuseIsRejectedByCodeWithoutAskingTheModel() = runTest {
    val long = List(130) { "palabra" }.joinToString(" ")
    val local = FakeLocalBrain(happyResponses("severe"))
    val result = run(local, FakeCloudBrain(listOf(long, "corta")))
    assertEquals("corta", result.excuse)
    assertEquals(1, local.calls.count { it.first == StepId.REVIEW }) // only the second, short version reached the model
  }

  @Test fun sameStoryGetsDifferentThemes() = runTest {
    val themes = (1..8).map {
      val cloud = FakeCloudBrain()
      run(FakeLocalBrain(happyResponses("severe")), cloud)
      cloud.prompts.single().lines().first { line -> line.startsWith("Theme of the invented story:") }
    }.toSet()
    assertTrue(themes.size > 1)
  }

  @Test fun consecutiveRunsNeverRepeatTheTheme() = runTest {
    val cloud = FakeCloudBrain()
    val pipeline = ExcusePipeline(FakeLocalBrain(happyResponses("severe")), cloud, realPrompts, Deanonymizer(testGenerics))
    repeat(10) { pipeline.run(LAURA_STORY, Tone.AUTO, RoutingMode.AUTO, isOnline = true) {} }
    val themes = cloud.prompts.map { p -> p.lines().first { it.startsWith("Theme of the invented story:") } }
    assertTrue(themes.zipWithNext().none { (a, b) -> a == b })
  }
}
