package com.example.legendaryexcuse.pipeline

/**
 * CODE brain: puts the real names back into the excuse. Exact string replacement, no model, so it
 * cannot hallucinate. Unknown placeholders become a generic word from [generics], keyed by type
 * (PERSON, PLACE, COMPANY, DATE, OTHER). The generic words come from strings.xml.
 */
class Deanonymizer(private val generics: Map<String, String>) {
  private val placeholder = Regex("""\[([A-Z]+)_\d+]""")

  fun restore(text: String, map: Map<String, String>): String {
    var result = text
    for ((key, realValue) in map) result = result.replace(key, realValue)
    return placeholder.replace(result) { match ->
      generics[match.groupValues[1]] ?: generics.getValue("OTHER")
    }
  }
}
