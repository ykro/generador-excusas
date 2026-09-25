package com.example.legendaryexcuse.pipeline

import com.example.legendaryexcuse.model.Anonymization

/**
 * CODE brain: we don't blindly trust the small model. Before anything can leave the phone we check
 * (1) that no real value from the anonymization map is still in the text and
 * (2) that there are no phone numbers, e-mails or long numbers (6+ digits).
 * Anything found is replaced by a new placeholder that the de-anonymizer can restore later.
 */
object LeakDetector {
  private val patterns = listOf(
    Regex("""[\w.+-]+@[\w-]+\.[\w.]+"""), // e-mail
    Regex("""\+?\d[\d\s-]{6,}\d"""), // phone number
    Regex("""\d{6,}"""), // long number (ids, accounts)
  )

  data class Result(val anonymization: Anonymization, val fixed: Boolean)

  private fun wholeWord(value: String) =
    Regex("""(?<![\p{L}\p{N}])${Regex.escape(value)}(?![\p{L}\p{N}])""", RegexOption.IGNORE_CASE)

  fun check(input: Anonymization): Result {
    var text = input.text
    val map = input.map.toMutableMap()

    // 1. Real values the model forgot to replace. Whole words only: a short name like "Ana" must not match inside "mañana".
    for ((placeholder, realValue) in input.map) {
      if (realValue.isNotBlank()) text = wholeWord(realValue).replace(text) { placeholder }
    }

    // 2. Sensitive patterns.
    var counter = 1
    for (pattern in patterns) {
      text = pattern.replace(text) { match ->
        val placeholder = "[DATA_${counter++}]"
        map[placeholder] = match.value
        placeholder
      }
    }

    return Result(Anonymization(text, map), fixed = text != input.text)
  }
}
