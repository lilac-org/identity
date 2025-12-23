package com.lilac.identity.domain.service

interface MailService {
    fun send(to: String, subject: String, html: String)
}