package com.togethertrip.chat.global.logging

import org.junit.jupiter.api.Test
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.io.ClassPathResource
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LoggingConfigurationTest {

    @Test
    fun `console pattern에 채팅 MDC 키와 기본값을 포함한다`() {
        val propertySources = YamlPropertySourceLoader().load("application", ClassPathResource("application.yml"))
        val pattern = propertySources
            .asSequence()
            .mapNotNull { it.getProperty("logging.pattern.console") as? String }
            .firstOrNull()

        assertNotNull(pattern)
        REQUIRED_MDC_KEYS.forEach { key ->
            assertTrue(pattern.contains("%X{$key:-}"), "console pattern에 $key 기본값이 필요합니다.")
        }
    }

    companion object {
        private val REQUIRED_MDC_KEYS = listOf("requestId", "userId", "sessionId", "chatRoomId", "messageType")
    }
}
