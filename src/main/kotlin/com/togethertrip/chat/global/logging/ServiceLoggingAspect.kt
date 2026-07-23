package com.togethertrip.chat.global.logging

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Aspect
@Component
class ServiceLoggingAspect {

    @Around("within(com.togethertrip.chat..service..*) || within(com.togethertrip.chat..handler..*)")
    fun logChatExecution(joinPoint: ProceedingJoinPoint): Any? {
        val startedAt = System.nanoTime()
        val signature = joinPoint.signature as MethodSignature
        val logger = LoggerFactory.getLogger(signature.declaringType)
        val methodName = "${signature.declaringType.simpleName}.${signature.name}"

        if (logger.isDebugEnabled) {
            logger.debug(
                "chat method started method={} args={}",
                methodName,
                summarizeArgs(joinPoint.args),
            )
        }

        return try {
            val result = joinPoint.proceed()
            logger.info(
                "chat method completed method={} elapsedMs={}",
                methodName,
                elapsedMillis(startedAt),
            )
            result
        } catch (exception: Throwable) {
            logger.warn(
                "chat method failed method={} elapsedMs={} exception={} message={}",
                methodName,
                elapsedMillis(startedAt),
                exception::class.simpleName,
                exception.message?.let(SensitiveDataMasker::mask),
            )
            throw exception
        }
    }

    private fun summarizeArgs(args: Array<Any?>): String {
        return args.joinToString(prefix = "[", postfix = "]", limit = MAX_ARGUMENT_COUNT) {
            SensitiveDataMasker.summarize(it)
        }
    }

    private fun elapsedMillis(startedAt: Long): Long {
        return (System.nanoTime() - startedAt) / 1_000_000
    }

    companion object {
        private const val MAX_ARGUMENT_COUNT = 5
    }
}
