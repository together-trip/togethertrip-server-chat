package com.togethertrip.chat.service

import org.springframework.stereotype.Service

@Service
class LoggingProbeService {
    fun handle(message: String): String = "service-ok"
}
