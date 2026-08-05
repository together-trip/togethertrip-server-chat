package com.togethertrip.chat.global.logging

import java.lang.reflect.Array

object SafeLogValueSummarizer {

    fun summarize(value: Any?): String {
        return when (value) {
            null -> "null"
            is CharSequence -> "${typeName(value)}(length=${value.length})"
            is Char -> "Char"
            is Byte, is Short, is Int, is Long, is Float, is Double, is Boolean -> value.toString()
            is Enum<*> -> "${typeName(value)}.${value.name}"
            is Collection<*> -> "${typeName(value)}(size=${value.size})"
            is Map<*, *> -> "${typeName(value)}(size=${value.size})"
            else -> summarizeObject(value)
        }
    }

    private fun summarizeObject(value: Any): String {
        return if (value.javaClass.isArray) {
            "${typeName(value)}(size=${Array.getLength(value)})"
        } else {
            typeName(value)
        }
    }

    private fun typeName(value: Any): String {
        return value::class.simpleName ?: value.javaClass.simpleName.ifBlank { "Object" }
    }
}
