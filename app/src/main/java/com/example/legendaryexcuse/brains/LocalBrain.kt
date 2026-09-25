package com.example.legendaryexcuse.brains

import com.example.legendaryexcuse.model.StepId
import kotlinx.coroutines.flow.Flow

/** The small model that lives on the phone (Gemma). Private data only ever goes here. */
interface LocalBrain {
  /** Runs the agent for [step] with a filled prompt and returns its raw (JSON) answer. */
  suspend fun runJson(step: StepId, input: String): String

  /** Writes the excuse on-device, emitting text chunks as they are generated. */
  fun stream(input: String): Flow<String>
}
