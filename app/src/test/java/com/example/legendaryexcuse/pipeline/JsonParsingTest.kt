package com.example.legendaryexcuse.pipeline

import com.example.legendaryexcuse.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JsonParsingTest {
  @Test fun extractsJsonWrappedInProseAndFences() {
    val raw = "Sure! Here it is:\n```json\n{\"severity\": \"severe\", \"reason\": \"boss\"}\n```"
    assertEquals(SeverityJson("severe", "boss"), JsonParsing.parse<SeverityJson>(raw))
  }

  @Test fun parsesNestedMap() {
    val raw = """{"text": "Hola [PERSON_1]", "map": {"[PERSON_1]": "Laura"}}"""
    assertEquals(mapOf("[PERSON_1]" to "Laura"), JsonParsing.parse<AnonymizeJson>(raw)?.map)
  }

  @Test fun ignoresUnknownKeys() {
    assertEquals("x", JsonParsing.parse<SummaryJson>("""{"summary": "x", "extra": 1}""")?.summary)
  }

  @Test fun returnsNullForInvalidJson() {
    assertNull(JsonParsing.parse<SummaryJson>("no json here"))
    assertNull(JsonParsing.parse<ReviewJson>("{broken"))
  }

  @Test fun enumLookupIsCaseInsensitive() {
    assertEquals(Severity.SEVERE, JsonParsing.enumOrNull<Severity>(" Severe "))
    assertNull(JsonParsing.enumOrNull<Severity>("huge"))
  }
}
