package com.togethertrip.chat.global.logging

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SafeLogValueSummarizerTest {

    @Test
    fun `문자열과 객체는 원문 없이 타입 메타데이터만 반환한다`() {
        assertEquals("String(length=6)", SafeLogValueSummarizer.summarize("secret"))
        assertEquals("SecretPayload", SafeLogValueSummarizer.summarize(SecretPayload()))
    }

    @Test
    fun `collection map array는 원소 대신 크기만 반환한다`() {
        assertEquals("SingletonList(size=1)", SafeLogValueSummarizer.summarize(listOf("secret")))
        assertEquals("SingletonMap(size=1)", SafeLogValueSummarizer.summarize(mapOf("secret-key" to "secret-value")))
        assertEquals("IntArray(size=3)", SafeLogValueSummarizer.summarize(intArrayOf(1, 2, 3)))
    }

    @Test
    fun `안전한 primitive와 enum만 값을 반환한다`() {
        assertEquals("42", SafeLogValueSummarizer.summarize(42))
        assertEquals("true", SafeLogValueSummarizer.summarize(true))
        assertEquals("MessageType.TEXT", SafeLogValueSummarizer.summarize(MessageType.TEXT))
        assertEquals("Char", SafeLogValueSummarizer.summarize('x'))
    }

    private class SecretPayload {
        override fun toString(): String = error("임의 객체의 toString을 호출하면 안 됩니다.")
    }

    private enum class MessageType {
        TEXT,
    }
}
