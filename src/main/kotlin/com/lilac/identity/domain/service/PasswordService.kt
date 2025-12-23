package com.lilac.identity.domain.service

interface PasswordService {
    fun hash(password: String): String
    fun verify(password: String, hash: String): Boolean
}