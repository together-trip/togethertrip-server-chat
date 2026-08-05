package com.togethertrip.chat.global.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.slf4j.LoggerFactory
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServiceLoggingAspectTest {

    private val aspect = ServiceLoggingAspect()
    private val logger = LoggerFactory.getLogger(ServiceLoggingAspectTest::class.java) as Logger
    private val originalLevel = logger.level

    @AfterEach
    fun restoreLogger() {
        logger.level = originalLevel
    }

    @Test
    fun `채팅 메서드 결과를 그대로 반환한다`() {
        val joinPoint = mockJoinPoint(result = "ok")

        val result = aspect.logChatExecution(joinPoint)

        assertEquals("ok", result)
        verify(joinPoint).proceed()
    }

    @Test
    fun `채팅 메서드 예외를 다시 던진다`() {
        val exception = IllegalStateException("message=비밀대화")
        val joinPoint = mockJoinPoint(exception = exception)

        val thrown = try {
            aspect.logChatExecution(joinPoint)
            throw AssertionError("예외가 다시 던져져야 합니다.")
        } catch (thrown: IllegalStateException) {
            thrown
        }

        assertEquals(exception, thrown)
        verify(joinPoint).proceed()
    }

    @Test
    fun `문자열과 임의 DTO의 원문 대신 안전한 메타데이터만 기록한다`() {
        val appender = attachAppender()
        val joinPoint = mockJoinPoint(
            result = "ok",
            args = arrayOf("plain-message-secret", UnsafePayload("dto-message-secret"), listOf("list-secret")),
        )

        try {
            aspect.logChatExecution(joinPoint)

            val renderedLog = appender.list.joinToString("\n") { it.formattedMessage }
            assertFalse(renderedLog.contains("plain-message-secret"))
            assertFalse(renderedLog.contains("dto-message-secret"))
            assertFalse(renderedLog.contains("list-secret"))
            assertTrue(renderedLog.contains("String(length="))
            assertTrue(renderedLog.contains("UnsafePayload"))
            assertTrue(renderedLog.contains("size=1"))
        } finally {
            detachAppender(appender)
        }
    }

    @Test
    fun `예외 메시지 원문과 throwable을 로그에 남기지 않는다`() {
        val appender = attachAppender()
        val joinPoint = mockJoinPoint(exception = IllegalStateException("exception-message-secret"))

        try {
            try {
                aspect.logChatExecution(joinPoint)
            } catch (_: IllegalStateException) {
                // 예외 전파 계약은 별도 테스트와 함께 유지한다.
            }

            val renderedLog = appender.list.joinToString("\n") { event ->
                listOfNotNull(event.formattedMessage, event.throwableProxy?.message).joinToString(" ")
            }
            assertFalse(renderedLog.contains("exception-message-secret"))
            assertTrue(renderedLog.contains("exceptionType=IllegalStateException"))
            assertTrue(appender.list.all { it.throwableProxy == null })
        } finally {
            detachAppender(appender)
        }
    }

    private fun mockJoinPoint(
        result: Any? = null,
        exception: Throwable? = null,
        args: Array<Any?> = arrayOf("message=비밀대화"),
    ): ProceedingJoinPoint {
        val joinPoint = mock(ProceedingJoinPoint::class.java)
        val signature = mock(MethodSignature::class.java)

        `when`(signature.declaringType).thenReturn(ServiceLoggingAspectTest::class.java)
        `when`(signature.name).thenReturn("sample")
        `when`(joinPoint.signature).thenReturn(signature)
        `when`(joinPoint.args).thenReturn(args)

        if (exception == null) {
            `when`(joinPoint.proceed()).thenReturn(result)
        } else {
            `when`(joinPoint.proceed()).thenThrow(exception)
        }

        return joinPoint
    }

    private fun attachAppender(): ListAppender<ILoggingEvent> {
        logger.level = Level.DEBUG
        return ListAppender<ILoggingEvent>().also {
            it.start()
            logger.addAppender(it)
        }
    }

    private fun detachAppender(appender: ListAppender<ILoggingEvent>) {
        logger.detachAppender(appender)
        appender.stop()
    }

    private data class UnsafePayload(val content: String)
}
