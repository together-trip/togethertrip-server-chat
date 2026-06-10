package com.togethertrip.chat.global.logging

import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class SensitiveDataMaskerTest {

    @Test
    fun `채팅 메시지 원문과 토큰을 마스킹한다`() {
        val masked = SensitiveDataMasker.mask("message=비밀대화 Authorization: Bearer abc.def")

        assertFalse(masked.contains("비밀대화"))
        assertFalse(masked.contains("abc.def"))
        assertContains(masked, "***")
    }

    @Test
    fun `전화번호와 이메일을 마스킹한다`() {
        val masked = SensitiveDataMasker.mask("010-1234-5678 user@example.com")

        assertFalse(masked.contains("010-1234-5678"))
        assertFalse(masked.contains("user@example.com"))
    }
}
