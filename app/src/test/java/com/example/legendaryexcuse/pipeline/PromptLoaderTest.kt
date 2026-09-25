package com.example.legendaryexcuse.pipeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptLoaderTest {
  @Test fun fillsVariables() {
    val loader = PromptLoader { "Hi {{name}}, {{name}}! {{missing}}" }
    assertEquals("Hi Ana, Ana! {{missing}}", loader.fill("x", mapOf("name" to "Ana")))
  }

  @Test fun baseTemplateHasNoLeftoverVariables() {
    val vars = listOf("summary", "context", "severity", "tone", "approach", "theme", "correction").associateWith { "v" }
    val prompt = realPrompts.fill("base_template", vars)
    assertFalse(prompt.contains("{{"))
    assertTrue(prompt.contains("NEVER reveal it): v"))
  }

  @Test fun correctionIncludesReason() {
    assertEquals("A reviewer rejected your previous version because: too long. Fix it.", realPrompts.fill("correction", mapOf("reason" to "too long")))
  }

  @Test fun themesAreLoadedAsLines() {
    val themes = realPrompts.lines("themes")
    assertTrue(themes.size >= 10)
    assertTrue(themes.none { it.isBlank() })
  }
}
