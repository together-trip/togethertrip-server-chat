package com.togethertrip.chat.global.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.togethertrip.chat.handler.LoggingProbeHandler
import com.togethertrip.chat.service.LoggingProbeService
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.aop.support.AopUtils
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServiceLoggingAspectProxyTest {

    @Test
    fun `Spring proxy가 Service와 Handler pointcut을 적용하고 원문은 남기지 않는다`() {
        AnnotationConfigApplicationContext(LoggingAspectProxyTestConfig::class.java).use { context ->
            val service = context.getBean(LoggingProbeService::class.java)
            val handler = context.getBean(LoggingProbeHandler::class.java)
            val serviceLogger = LoggerFactory.getLogger(LoggingProbeService::class.java) as Logger
            val handlerLogger = LoggerFactory.getLogger(LoggingProbeHandler::class.java) as Logger
            val serviceAppender = attachAppender(serviceLogger)
            val handlerAppender = attachAppender(handlerLogger)

            try {
                assertTrue(AopUtils.isAopProxy(service))
                assertTrue(AopUtils.isAopProxy(handler))
                assertEquals("service-ok", service.handle("service-plain-secret"))
                assertEquals("handler-ok", handler.handle("handler-plain-secret"))

                val renderedLog = (serviceAppender.list + handlerAppender.list)
                    .joinToString("\n") { it.formattedMessage }
                assertFalse(renderedLog.contains("service-plain-secret"))
                assertFalse(renderedLog.contains("handler-plain-secret"))
                assertTrue(renderedLog.contains("LoggingProbeService.handle"))
                assertTrue(renderedLog.contains("LoggingProbeHandler.handle"))
            } finally {
                detachAppender(serviceLogger, serviceAppender)
                detachAppender(handlerLogger, handlerAppender)
            }
        }
    }

    private fun attachAppender(logger: Logger): ListAppender<ILoggingEvent> {
        logger.level = Level.DEBUG
        return ListAppender<ILoggingEvent>().also {
            it.start()
            logger.addAppender(it)
        }
    }

    private fun detachAppender(logger: Logger, appender: ListAppender<ILoggingEvent>) {
        logger.detachAppender(appender)
        logger.level = null
        appender.stop()
    }
}

@Configuration(proxyBeanMethods = false)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Import(ServiceLoggingAspect::class, LoggingProbeService::class, LoggingProbeHandler::class)
private class LoggingAspectProxyTestConfig
