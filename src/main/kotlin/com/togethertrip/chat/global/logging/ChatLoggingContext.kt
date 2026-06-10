package com.togethertrip.chat.global.logging

import org.slf4j.MDC

object ChatLoggingContext {
    const val REQUEST_ID = "requestId"
    const val USER_ID = "userId"
    const val SESSION_ID = "sessionId"
    const val CHAT_ROOM_ID = "chatRoomId"
    const val MESSAGE_TYPE = "messageType"

    fun putUser(userId: String?) {
        MDC.put(USER_ID, userId?.takeIf { it.isNotBlank() } ?: "anonymous")
    }

    fun putSession(sessionId: String?) {
        sessionId?.takeIf { it.isNotBlank() }?.let { MDC.put(SESSION_ID, it) }
    }

    fun putChatRoom(chatRoomId: String?) {
        chatRoomId?.takeIf { it.isNotBlank() }?.let { MDC.put(CHAT_ROOM_ID, it) }
    }

    fun putMessageType(messageType: String?) {
        messageType?.takeIf { it.isNotBlank() }?.let { MDC.put(MESSAGE_TYPE, it) }
    }

    fun clearChatScope() {
        MDC.remove(SESSION_ID)
        MDC.remove(CHAT_ROOM_ID)
        MDC.remove(MESSAGE_TYPE)
    }
}
