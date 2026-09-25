package com.example.legendaryexcuse.brains

import com.example.legendaryexcuse.agents.ExcuseAgents
import com.example.legendaryexcuse.agents.ask
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.FirebaseAI
import kotlinx.coroutines.withTimeout
import com.google.adk.firebase.models.Firebase as AdkFirebase

const val GEMINI_MODEL = "gemini-3.8-flash"

/**
 * Gemini through Firebase AI Logic (Gemini Developer API backend), wrapped as an ADK model.
 * No API key in the app: requests are authorized by Firebase + App Check.
 */
class GeminiCloudBrain(firebaseAi: FirebaseAI = FirebaseAI.getInstance(FirebaseApp.getInstance())) : CloudBrain {
  private val writer = ExcuseAgents.cloudWriter(AdkFirebase.create(GEMINI_MODEL, firebaseAi))

  override suspend fun generate(prompt: String): String = withTimeout(20_000) {
    writer.ask(prompt).also { check(it.isNotBlank()) { "Empty response from $GEMINI_MODEL" } }
  }
}
