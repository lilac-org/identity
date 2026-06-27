package id.andreasmlbngaol.identity.data.security

import id.andreasmlbngaol.identity.data.config.JwtKeyConfig
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * Parses the configured PEM key material into typed RSA keys used by the JWT
 * issuer/verifier. The private key must be PKCS#8 (`BEGIN PRIVATE KEY`) and the
 * public key X.509/SubjectPublicKeyInfo (`BEGIN PUBLIC KEY`).
 */
class RsaKeys(config: JwtKeyConfig) {

    /** Stable identifier published in JWT headers and the JWKS document. */
    val keyId: String = config.keyId
    val privateKey: RSAPrivateKey = parsePrivate(config.privateKeyPem)
    val publicKey: RSAPublicKey = parsePublic(config.publicKeyPem)

    private fun parsePrivate(pem: String): RSAPrivateKey {
        val der = decode(pem, "PRIVATE KEY")
        val spec = PKCS8EncodedKeySpec(der)
        return KeyFactory.getInstance("RSA").generatePrivate(spec) as RSAPrivateKey
    }

    private fun parsePublic(pem: String): RSAPublicKey {
        val der = decode(pem, "PUBLIC KEY")
        val spec = X509EncodedKeySpec(der)
        return KeyFactory.getInstance("RSA").generatePublic(spec) as RSAPublicKey
    }

    /** Strips the PEM armor and whitespace, then base64-decodes the body. */
    private fun decode(pem: String, type: String): ByteArray {
        val body = pem
            .replace("-----BEGIN $type-----", "")
            .replace("-----END $type-----", "")
            .replace(Regex("\\s"), "")
        return Base64.getDecoder().decode(body)
    }
}
