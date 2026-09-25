package com.example.legendaryexcuse.pipeline

import com.example.legendaryexcuse.brains.CloudBrain
import com.example.legendaryexcuse.brains.LocalBrain
import com.example.legendaryexcuse.model.StepId
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Reads the real prompt files from src/main/assets (unit tests run with the module as working dir). */
val realPrompts = PromptLoader { name -> File("src/main/assets/prompts/$name.txt").readText() }

val testGenerics = mapOf("PERSON" to "alguien", "PLACE" to "un lugar", "COMPANY" to "una empresa", "DATE" to "ese día", "OTHER" to "algo")

/** Local brain that answers each step from a queue of canned responses and records its inputs. */
class FakeLocalBrain(responses: Map<StepId, List<String>>, private val excuse: String = "Lo siento [PERSON_1], hubo un eclipse.") : LocalBrain {
  private val queues = responses.mapValues { ArrayDeque(it.value) }
  val calls = mutableListOf<Pair<StepId, String>>()
  var streamed = 0

  override suspend fun runJson(step: StepId, input: String): String {
    calls += step to input
    val queue = queues[step] ?: return "not json"
    return if (queue.size > 1) queue.removeFirst() else queue.first()
  }

  override fun stream(input: String): Flow<String> {
    streamed++
    return flowOf(*excuse.chunked(5).toTypedArray())
  }
}

class FakeCloudBrain(private val answers: List<String> = listOf("Perdón [PERSON_1], un dragón bloqueó [PLACE_1]."), private val error: Exception? = null) : CloudBrain {
  val prompts = mutableListOf<String>()
  override suspend fun generate(prompt: String): String {
    prompts += prompt
    error?.let { throw it }
    return answers[minOf(prompts.size - 1, answers.lastIndex)]
  }
}

const val LAURA_STORY = "Me quedé dormido viendo series y no llegué a la junta con mi jefa Laura en Oficinas Norte"

fun happyResponses(severity: String) = mapOf(
  StepId.ANONYMIZE to listOf("""{"text": "Me quedé dormido viendo series y no llegué a la junta con mi jefa [PERSON_1] en [PLACE_1]", "map": {"[PERSON_1]": "Laura", "[PLACE_1]": "Oficinas Norte"}}"""),
  StepId.SEVERITY to listOf("""{"severity": "$severity", "reason": "r"}"""),
  StepId.STYLE to listOf("""{"context": "work", "tone": "formal"}"""),
  StepId.PROMPT to listOf("""{"approach": "Blame the weather."}"""),
  StepId.REVIEW to listOf("""{"approved": true, "reason": "ok"}"""),
)
