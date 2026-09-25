package com.example.legendaryexcuse.pipeline

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class AnonymizeJson(val text: String, val map: Map<String, String> = emptyMap())
@Serializable data class SummaryJson(val summary: String)
@Serializable data class SeverityJson(val severity: String, val reason: String = "")
@Serializable data class StyleJson(val context: String = "", val tone: String = "")
@Serializable data class ApproachJson(val approach: String = "")
@Serializable data class ReviewJson(val approved: Boolean, val reason: String = "")

/** Small models often wrap JSON in prose or ```json fences, so we cut out the outermost {...}. */
object JsonParsing {
  val json = Json { ignoreUnknownKeys = true; isLenient = true }

  fun extractObject(text: String): String? {
    val start = text.indexOf('{')
    val end = text.lastIndexOf('}')
    return if (start >= 0 && end > start) text.substring(start, end + 1) else null
  }

  inline fun <reified T> parse(text: String): T? =
    extractObject(text)?.let { runCatching { json.decodeFromString<T>(it) }.getOrNull() }

  /** Case-insensitive enum lookup, e.g. "Severe" -> Severity.SEVERE. */
  inline fun <reified E : Enum<E>> enumOrNull(value: String): E? =
    enumValues<E>().firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
}
