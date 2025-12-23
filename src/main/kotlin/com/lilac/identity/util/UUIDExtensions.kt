package com.lilac.identity.util

import java.util.UUID

fun String.toUUID(): UUID = try {
    UUID.fromString(this)
} catch (_: Exception) {
    throw IllegalArgumentException("Invalid UUID: $this")
}