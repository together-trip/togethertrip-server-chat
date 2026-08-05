package com.togethertrip.chat.global.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RequestLoggingFilterTest {

    private val filter = RequestLoggingFilter(RequestIdGenerator())

    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun `요청 ID가 없으면 생성하고 응답 헤더로 반환한다`() {
        val request = MockHttpServletRequest("GET", "/chat/health")
        val response = MockHttpServletResponse()
        var requestIdInChain: String? = null

        filter.doFilter(request, response, FilterChain { _, _ ->
            requestIdInChain = MDC.get(ChatLoggingContext.REQUEST_ID)
        })

        assertNotNull(requestIdInChain)
        assertTrue(requestIdInChain!!.isNotBlank())
        assertEquals(requestIdInChain, response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER))
        assertNull(MDC.get(ChatLoggingContext.REQUEST_ID))
    }

    @Test
    fun `기존 요청 ID를 유지한다`() {
        val request = MockHttpServletRequest("GET", "/chat/rooms").apply {
            addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "request-123")
        }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, FilterChain { _, _ ->
            assertEquals("request-123", MDC.get(ChatLoggingContext.REQUEST_ID))
        })

        assertEquals("request-123", response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER))
    }

    @Test
    fun `허용되지 않은 요청 ID는 새 UUID로 교체한다`() {
        val invalidRequestId = "bad request id/with?characters"
        val request = MockHttpServletRequest("GET", "/chat/rooms").apply {
            addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, invalidRequestId)
        }
        val response = MockHttpServletResponse()
        var requestIdInChain: String? = null

        filter.doFilter(request, response, FilterChain { _, _ ->
            requestIdInChain = MDC.get(ChatLoggingContext.REQUEST_ID)
        })

        assertNotEquals(invalidRequestId, requestIdInChain)
        assertTrue(requireNotNull(requestIdInChain).matches(UUID_PATTERN))
        assertEquals(requestIdInChain, response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER))
    }

    @Test
    fun `요청 처리 후 기존 MDC를 복원한다`() {
        MDC.put("traceId", "outer-trace")
        MDC.put(ChatLoggingContext.REQUEST_ID, "outer-request")
        val request = MockHttpServletRequest("GET", "/chat/rooms")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, FilterChain { _, _ ->
            assertNotEquals("outer-request", MDC.get(ChatLoggingContext.REQUEST_ID))
        })

        assertEquals("outer-trace", MDC.get("traceId"))
        assertEquals("outer-request", MDC.get(ChatLoggingContext.REQUEST_ID))
    }

    @Test
    fun `실패 로그에 query header body 예외 원문을 남기지 않는다`() {
        val logger = LoggerFactory.getLogger(RequestLoggingFilter::class.java) as Logger
        val originalLevel = logger.level
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.level = Level.INFO
        logger.addAppender(appender)
        val request = MockHttpServletRequest("POST", "/chat/messages").apply {
            queryString = "travelerName=query-secret"
            addHeader("Authorization", "Bearer header-secret")
            setContent("body-secret".toByteArray())
        }
        val response = MockHttpServletResponse()

        try {
            try {
                filter.doFilter(request, response, FilterChain { _, _ ->
                    throw IllegalStateException("exception-secret")
                })
            } catch (_: IllegalStateException) {
                // 필터가 예외를 삼키지 않는 계약은 유지한다.
            }

            val renderedLog = appender.list.joinToString("\n") { event ->
                listOfNotNull(event.formattedMessage, event.throwableProxy?.message).joinToString(" ")
            }
            assertContainsSafely(renderedLog, "method=POST")
            assertContainsSafely(renderedLog, "path=/chat/messages")
            assertContainsSafely(renderedLog, "exceptionType=IllegalStateException")
            assertFalse(renderedLog.contains("travelerName"))
            assertFalse(renderedLog.contains("query-secret"))
            assertFalse(renderedLog.contains("header-secret"))
            assertFalse(renderedLog.contains("body-secret"))
            assertFalse(renderedLog.contains("exception-secret"))
            assertNull(appender.list.single().throwableProxy)
        } finally {
            logger.detachAppender(appender)
            logger.level = originalLevel
            appender.stop()
        }
    }

    private fun assertContainsSafely(actual: String, expected: String) {
        assertTrue(actual.contains(expected), "expected <$expected> in <$actual>")
    }

    companion object {
        private val UUID_PATTERN = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
    }
}
