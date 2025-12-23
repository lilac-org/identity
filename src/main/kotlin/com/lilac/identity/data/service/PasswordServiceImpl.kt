package com.lilac.identity.data.service

import com.lilac.identity.domain.service.PasswordService
import org.mindrot.jbcrypt.BCrypt

class PasswordServiceImpl(): PasswordService {
    override fun hash(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt())
    override fun verify(password: String, hash: String) = BCrypt.checkpw(password, hash)
}