package com.togethertrip.chat.handler

import org.springframework.stereotype.Component

@Component
class LoggingProbeHandler {
    fun handle(message: String): String = "handler-ok"
}
