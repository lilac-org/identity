package id.andreasmlbngaol.identity.data.security

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import id.andreasmlbngaol.identity.domain.enums.TokenType
import id.andreasmlbngaol.identity.domain.error.ErrorCode
import id.andreasmlbngaol.identity.domain.error.TokenException
import id.andreasmlbngaol.identity.domain.model.AccessTokenClaims
import id.andreasmlbngaol.identity.domain.model.User
import id.andreasmlbngaol.identity.domain.policy.AuthPolicy
import id.andreasmlbngaol.identity.domain.service.Clock
import id.andreasmlbngaol.identity.domain.service.TokenIssuer
import kotlin.time.Instant
import java.util.Date
import java.util.UUID
import kotlin.uuid.Uuid

/**
 * RS256 JWT issuer/verifier built on nimbus-jose-jwt.
 *
 * Access tokens are stateless: downstream services verify them with the public
 * key from the JWKS endpoint without calling this service. The `tokenVersion`
 * claim lets us cheaply invalidate all of a user's tokens (logout-everywhere,
 * password change/reset) by bumping the stored version.
 */
class JwtTokenIssuer(
    private val keys: RsaKeys,
    private val policy: AuthPolicy,
    private val clock: Clock,
) : TokenIssuer {

    private val signer = RSASSASigner(keys.privateKey)
    private val verifier = RSASSAVerifier(keys.publicKey)

    /** The JWKS document (public keys only) served at /.well-known/jwks.json. */
    fun jwks(): Map<String, Any> {
        val rsaKey = RSAKey.Builder(keys.publicKey)
            .keyID(keys.keyId)
            .algorithm(JWSAlgorithm.RS256)
            .build()
        return JWKSet(rsaKey).toJSONObject(true)
    }

    override fun issueAccessToken(user: User, audiences: Set<String>): TokenIssuer.IssuedAccessToken {
        val now = clock.now()
        val exp = now.plus(policy.accessTokenTtl)
        val claims = baseClaims(now, exp, audiences)
            .subject(user.id.toString())
            .claim("type", TokenType.ACCESS.name)
            .claim("tokenVersion", user.tokenVersion)
            .claim("roles", user.roleNames.toList())
            .claim("permissions", user.permissionNames.toList())
            .claim("email", user.email)
            .claim("username", user.username)
            .build()
        return TokenIssuer.IssuedAccessToken(sign(claims), exp)
    }

    override fun issueServiceToken(
        clientId: String,
        scopes: Set<String>,
        audiences: Set<String>,
    ): TokenIssuer.IssuedServiceToken {
        val now = clock.now()
        val exp = now.plus(policy.serviceTokenTtl)
        val claims = baseClaims(now, exp, audiences)
            .subject(clientId)
            .claim("type", TokenType.SERVICE.name)
            .claim("scopes", scopes.toList())
            .claim("permissions", scopes.toList())
            .build()
        return TokenIssuer.IssuedServiceToken(sign(claims), exp)
    }

    override fun generateRefreshToken(): String {
        // 256 bits of entropy, URL-safe.
        val bytes = ByteArray(32)
        SecureRandomProvider.instance.nextBytes(bytes)
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    override fun verifyAccessToken(token: String): AccessTokenClaims {
        val jwt = try {
            SignedJWT.parse(token)
        } catch (e: Exception) {
            throw TokenException(ErrorCode.TOKEN_INVALID, "Malformed token")
        }
        if (!jwt.verify(verifier)) throw TokenException(ErrorCode.TOKEN_INVALID, "Bad signature")
        val claims = jwt.jwtClaimsSet
        val now = Date.from(java.time.Instant.ofEpochMilli(clock.now().toEpochMilliseconds()))
        if (claims.expirationTime == null || claims.expirationTime.before(now)) {
            throw TokenException(ErrorCode.TOKEN_EXPIRED, "Token expired")
        }
        if (claims.issuer != policy.issuer) throw TokenException(ErrorCode.TOKEN_INVALID, "Bad issuer")

        val type = runCatching { TokenType.valueOf(claims.getStringClaim("type")) }
            .getOrElse { throw TokenException(ErrorCode.TOKEN_INVALID, "Unknown token type") }
        val subject = runCatching { Uuid.parse(claims.subject) }
            .getOrElse { Uuid.NIL }
        return AccessTokenClaims(
            subject = subject,
            tokenType = type,
            tokenVersion = (claims.getIntegerClaim("tokenVersion") ?: 0),
            roles = (claims.getStringListClaim("roles") ?: emptyList()).toSet(),
            permissions = (claims.getStringListClaim("permissions") ?: emptyList()).toSet(),
            audiences = (claims.audience ?: emptyList()).toSet(),
            tokenId = claims.jwtid ?: "",
        )
    }

    private fun baseClaims(now: Instant, exp: Instant, audiences: Set<String>): JWTClaimsSet.Builder =
        JWTClaimsSet.Builder()
            .issuer(policy.issuer)
            .issueTime(Date(now.toEpochMilliseconds()))
            .expirationTime(Date(exp.toEpochMilliseconds()))
            .jwtID(UUID.randomUUID().toString())
            .audience(audiences.ifEmpty { setOf(policy.defaultAudience) }.toList())

    private fun sign(claims: JWTClaimsSet): String {
        val header = JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(keys.keyId)
            .type(JOSEObjectType.JWT)
            .build()
        return SignedJWT(header, claims).apply { sign(signer) }.serialize()
    }
}
