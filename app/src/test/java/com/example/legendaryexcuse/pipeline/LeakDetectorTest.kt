package com.example.legendaryexcuse.pipeline

import com.example.legendaryexcuse.model.Anonymization
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeakDetectorTest {
  @Test fun cleanTextIsUntouched() {
    val input = Anonymization("Llegué tarde con [PERSON_1]", mapOf("[PERSON_1]" to "Laura"))
    val result = LeakDetector.check(input)
    assertFalse(result.fixed)
    assertEquals(input, result.anonymization)
  }

  @Test fun replacesRealValueTheModelForgot() {
    val input = Anonymization("Vi a [PERSON_1] y a laura", mapOf("[PERSON_1]" to "Laura"))
    val result = LeakDetector.check(input)
    assertTrue(result.fixed)
    assertEquals("Vi a [PERSON_1] y a [PERSON_1]", result.anonymization.text)
  }

  @Test fun redactsEmailPhoneAndLongNumbers() {
    val input = Anonymization("Escribe a ana@mail.com o al +502 5555-1234, cuenta 12345678", emptyMap())
    val result = LeakDetector.check(input)
    assertTrue(result.fixed)
    val text = result.anonymization.text
    assertFalse(text.contains("ana@mail.com"))
    assertFalse(text.contains("5555"))
    assertFalse(text.contains("12345678"))
    assertEquals("ana@mail.com", result.anonymization.map["[DATA_1]"])
  }

  @Test fun shortNumbersAreFine() {
    assertFalse(LeakDetector.check(Anonymization("Llegué 5 minutos tarde a las 4 am", emptyMap())).fixed)
  }
}
