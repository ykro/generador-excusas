package com.example.legendaryexcuse.agents

import com.example.legendaryexcuse.model.StepId
import com.google.adk.kt.agents.Instruction
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.agents.RunConfig
import com.google.adk.kt.agents.StreamingMode
import com.google.adk.kt.models.Model
import com.google.adk.kt.runners.InMemoryRunner
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.GenerateContentConfig
import com.google.adk.kt.types.Role
import com.google.adk.kt.types.ThinkingConfig
import com.google.adk.kt.types.ThinkingLevel
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList

/**
 * One ADK [LlmAgent] per step. Each agent has its own instruction (its role) and its own model.
 * The step's detailed prompt (assets/prompts/<step>.txt, already filled) is sent as the user message.
 */
object ExcuseAgents {

  private const val JSON_ONLY = "You run on the user's phone. Do exactly one task and reply only with JSON."

  private val localInstructions = mapOf(
    StepId.ANONYMIZE to "You are a privacy filter. $JSON_ONLY",
    StepId.SUMMARIZE to "You summarize stories without losing key facts. $JSON_ONLY",
    StepId.SEVERITY to "You classify how serious a situation is. $JSON_ONLY",
    StepId.STYLE to "You classify the context and tone of a situation. $JSON_ONLY",
    StepId.PROMPT to "You are an excuse strategist. $JSON_ONLY",
    StepId.REVIEW to "You are a strict quality reviewer of excuses. $JSON_ONLY",
    StepId.GENERATE to "You write short, epic excuses in Spanish that never reveal the real reason.",
  )

  /** All local agents share the SAME model instance (one LiteRT-LM engine for the whole app). */
  fun local(model: Model): Map<StepId, LlmAgent> =
    localInstructions.mapValues { (step, instruction) ->
      LlmAgent(name = "local_${step.name.lowercase()}", model = model, instruction = Instruction(instruction))
    }

  fun cloudWriter(model: Model) = LlmAgent(
    name = "cloud_writer",
    model = model,
    instruction = Instruction("You write short, epic excuses in Spanish that never reveal the real reason."),
    generateContentConfig = GenerateContentConfig(
      temperature = 0.9f,
      // Thinking tokens count against the output budget, so keep thinking LOW and leave room for the excuse.
      maxOutputTokens = 1024,
      thinkingConfig = ThinkingConfig(thinkingLevel = ThinkingLevel.LOW),
    ),
  )
}

/**
 * Runs an agent with ADK: a fresh [InMemoryRunner] and a new session per call, so one step never
 * contaminates the context of the next. Emits text chunks when [streaming], or the final text once.
 */
fun LlmAgent.runText(input: String, streaming: Boolean = false): Flow<String> = flow {
  val runner = InMemoryRunner(agent = this@runText, appName = "legendary_excuse")
  runner.runAsync(
    userId = "user",
    sessionId = UUID.randomUUID().toString(),
    newMessage = Content.fromText(Role.USER, input),
    runConfig = RunConfig(streamingMode = if (streaming) StreamingMode.SSE else StreamingMode.NONE),
  ).collect { event ->
    event.errorMessage?.let { throw IllegalStateException("${event.errorCode ?: "error"}: $it") }
    if (event.author != name) return@collect
    when {
      streaming && event.partial -> emit(event.contentText())
      !streaming && event.isFinalResponse -> emit(event.contentText())
    }
  }
}

/** Convenience: run and wait for the complete answer. */
suspend fun LlmAgent.ask(input: String): String = runText(input).toList().joinToString("")
