package com.togethertrip.chat.global.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class RequestLoggingFilter(
    private val requestIdGenerator: RequestIdGenerator,
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(RequestLoggingFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val startedAt = System.nanoTime()
        val requestId = resolveRequestId(request)
        val previousMdc = MDC.getCopyOfContextMap()
        var failure: Throwable? = null

        MDC.put(ChatLoggingContext.REQUEST_ID, requestId)
        ChatLoggingContext.putUser(null)
        response.setHeader(REQUEST_ID_HEADER, requestId)

        try {
            filterChain.doFilter(request, response)
        } catch (exception: Throwable) {
            failure = exception
            throw exception
        } finally {
            try {
                logRequest(request, response, elapsedMillis(startedAt), failure)
            } finally {
                restoreMdc(previousMdc)
            }
        }
    }

    private fun resolveRequestId(request: HttpServletRequest): String {
        return request.getHeader(REQUEST_ID_HEADER)
            ?.takeIf(REQUEST_ID_PATTERN::matches)
            ?: requestIdGenerator.generate()
    }

    private fun logRequest(
        request: HttpServletRequest,
        response: HttpServletResponse,
        elapsedMs: Long,
        failure: Throwable?,
    ) {
        val path = request.requestURI

        if (failure == null) {
            log.info(
                "chat http request completed method={} path={} status={} elapsedMs={}",
                request.method,
                path,
                response.status,
                elapsedMs,
            )
        } else {
            log.error(
                "chat http request failed method={} path={} status={} elapsedMs={} exceptionType={}",
                request.method,
                path,
                response.status,
                elapsedMs,
                failure::class.simpleName,
            )
        }
    }

    private fun restoreMdc(previousMdc: Map<String, String>?) {
        if (previousMdc.isNullOrEmpty()) {
            MDC.clear()
        } else {
            MDC.setContextMap(previousMdc)
        }
    }

    private fun elapsedMillis(startedAt: Long): Long {
        return (System.nanoTime() - startedAt) / 1_000_000
    }

    companion object {
        const val REQUEST_ID_HEADER = "X-Request-Id"
        private const val MAX_REQUEST_ID_LENGTH = 100
        private val REQUEST_ID_PATTERN = Regex("[A-Za-z0-9._:-]{1,$MAX_REQUEST_ID_LENGTH}")
    }
}
