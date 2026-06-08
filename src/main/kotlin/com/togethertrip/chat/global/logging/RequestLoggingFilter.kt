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
            logRequest(request, response, elapsedMillis(startedAt), failure)
            MDC.clear()
        }
    }

    private fun resolveRequestId(request: HttpServletRequest): String {
        return request.getHeader(REQUEST_ID_HEADER)
            ?.takeIf { it.isNotBlank() }
            ?.take(MAX_REQUEST_ID_LENGTH)
            ?: requestIdGenerator.generate()
    }

    private fun logRequest(
        request: HttpServletRequest,
        response: HttpServletResponse,
        elapsedMs: Long,
        failure: Throwable?,
    ) {
        val queryString = request.queryString
            ?.let(SensitiveDataMasker::mask)
            ?.let { "?$it" }
            ?: ""
        val path = "${request.requestURI}$queryString"

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
                "chat http request failed method={} path={} status={} elapsedMs={} exception={}",
                request.method,
                path,
                response.status,
                elapsedMs,
                failure::class.simpleName,
                failure,
            )
        }
    }

    private fun elapsedMillis(startedAt: Long): Long {
        return (System.nanoTime() - startedAt) / 1_000_000
    }

    companion object {
        const val REQUEST_ID_HEADER = "X-Request-Id"
        private const val MAX_REQUEST_ID_LENGTH = 100
    }
}
