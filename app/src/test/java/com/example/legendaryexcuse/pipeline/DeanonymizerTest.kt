package com.example.legendaryexcuse.pipeline

import org.junit.Assert.assertEquals
import org.junit.Test

class DeanonymizerTest {
  private val deanonymizer = Deanonymizer(testGenerics)

  @Test fun restoresKnownPlaceholders() {
    val map = mapOf("[PERSON_1]" to "Laura", "[PLACE_1]" to "Oficinas Norte")
    assertEquals("Laura, un tren a Oficinas Norte", deanonymizer.restore("[PERSON_1], un tren a [PLACE_1]", map))
  }

  @Test fun unknownPlaceholdersBecomeGenericWords() {
    assertEquals("alguien fue a un lugar ese día con algo", deanonymizer.restore("[PERSON_9] fue a [PLACE_3] [DATE_1] con [THING_1]", emptyMap()))
  }
}
