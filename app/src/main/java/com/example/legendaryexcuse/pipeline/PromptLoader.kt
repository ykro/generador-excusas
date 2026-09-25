package com.example.legendaryexcuse.pipeline

/**
 * Loads prompt templates from `assets/prompts/<name>.txt` and fills `{{variables}}`.
 * [readTemplate] is injected so tests can read the real files without Android.
 */
class PromptLoader(private val readTemplate: (name: String) -> String) {

  fun fill(name: String, vars: Map<String, String>): String {
    var text = readTemplate(name)
    for ((key, value) in vars) text = text.replace("{{$key}}", value)
    return text.trim()
  }

  /** Non-empty lines of a file, e.g. the list of story themes. */
  fun lines(name: String): List<String> = readTemplate(name).lines().map { it.trim() }.filter { it.isNotEmpty() }
}
