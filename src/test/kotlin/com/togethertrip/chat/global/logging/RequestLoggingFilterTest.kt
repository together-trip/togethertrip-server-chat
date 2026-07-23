package com.togethertrip.chat.global.logging

import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
}
