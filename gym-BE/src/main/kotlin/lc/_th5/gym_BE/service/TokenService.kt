package lc._th5.gym_BE.service

import lc._th5.gym_BE.config.JwtProperties
import lc._th5.gym_BE.model.user.RefreshToken
import lc._th5.gym_BE.model.user.User
import lc._th5.gym_BE.model.user.BlacklistedToken
import lc._th5.gym_BE.repository.RefreshTokenRepository
import lc._th5.gym_BE.repository.BlacklistedTokenRepository
import org.springframework.stereotype.Service
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class TokenService(
    private val jwtEncoder: JwtEncoder,
    private val jwtProperties: JwtProperties,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val blacklistedTokenRepository: BlacklistedTokenRepository
) {
    fun generateAccessToken(user: User): Pair<String, Long> {
        val now = Instant.now()
        val expiresAt = now.plusSeconds(jwtProperties.expirationSeconds)

        val scope = user.roles.joinToString(" ") { "ROLE_${it.name}" }

        val claims = JwtClaimsSet.builder()
            .issuer("gym-api")
            .issuedAt(now)
            .expiresAt(expiresAt)
            .subject(user.email)
            .id(UUID.randomUUID().toString()) // JTI for uniqueness
            .claim("uid", user.id)
            .claim("name", user.fullName)
            .claim("scope", scope)
            .build()
            
        val jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build() // Header with algorithm
        val params = JwtEncoderParameters.from(jwsHeader, claims) // Combine header and claims
        val token = jwtEncoder.encode(params).tokenValue // Encode to get the token string
        val ttl = jwtProperties.expirationSeconds // Time to live in seconds
        return token to ttl
    }

    fun generateRefreshToken(user: User): String {
        val now = Instant.now()
        val expiresAt = now.plusSeconds(jwtProperties.refreshExpirationSeconds)

        val claims = JwtClaimsSet.builder()
            .issuer("gym-api")
            .issuedAt(now)
            .expiresAt(expiresAt)
            .subject(user.email)
            .id(UUID.randomUUID().toString()) // JTI for uniqueness
            .claim("uid", user.id)
            .build()

        val jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build() // Header with algorithm
        val params = JwtEncoderParameters.from(jwsHeader, claims) // Combine header and claims
        val token = jwtEncoder.encode(params).tokenValue // Encode to get the token string

        // Save to database
        val refreshToken = RefreshToken(
            token = token,
            user = user,
            expiresAt = LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC)
        )
        refreshTokenRepository.save(refreshToken)

        return token
    }

    fun generateAccessTokenFromRefreshToken(refreshToken: String): Triple<String, Long, String>? {
        val storedToken = refreshTokenRepository.findByToken(refreshToken) 
            ?: return null

        // Check if expired
        if (storedToken.expiresAt.isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            refreshTokenRepository.delete(storedToken)
            return null
        }

        // Delete old refresh token (rotation)
        refreshTokenRepository.delete(storedToken)

        // Generate new access token
        val (newAccessToken, expiresIn) = generateAccessToken(storedToken.user)

        // Generate new refresh token
        val newRefreshToken = generateRefreshToken(storedToken.user)

        return Triple(newAccessToken, expiresIn, newRefreshToken)
    }

    fun blacklistToken(jti: String, expiresAt: LocalDateTime) {
        if (!blacklistedTokenRepository.existsByJti(jti)) {
            val blacklistedToken = BlacklistedToken(jti = jti, expiresAt = expiresAt)
            blacklistedTokenRepository.save(blacklistedToken)
        }
    }

    fun isTokenBlacklisted(jti: String): Boolean {
        return blacklistedTokenRepository.existsByJti(jti)
    }

    fun getRefreshTokenEntity(token: String): RefreshToken? {
        return refreshTokenRepository.findByToken(token)
    }
}