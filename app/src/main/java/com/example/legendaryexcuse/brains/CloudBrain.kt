package com.example.legendaryexcuse.brains

/** The big model in the cloud (Gemini). Only receives anonymized, summarized text. */
interface CloudBrain {
  suspend fun generate(prompt: String): String
}
