package lc._th5.gym_BE.config

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.OctetSequenceKey
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.nio.charset.StandardCharsets
import javax.crypto.spec.SecretKeySpec

@Configuration
class JwtConfig(
    private val jwtProperties: JwtProperties
) {

    private fun secretKeySpec(): SecretKeySpec {
        // Chuyển đổi chuỗi bí mật thành SecretKeySpec
        // Đảm bảo rằng chuỗi bí mật đủ dài cho thuật toán HS256 (ít nhất 32 ký tự)
        val keyBytes = jwtProperties.secret.toByteArray(StandardCharsets.UTF_8)
        // Nếu chuỗi bí mật ngắn hơn 64 byte, ta sẽ padding nó
        // Nếu dài hơn, ta sẽ cắt bớt
        val paddedKeyBytes = if (keyBytes.size < 32) {
            keyBytes + ByteArray(32 - keyBytes.size) { 0 } // Padding với byte 0 nếu cần
        } else {
            keyBytes.copyOf(32) // Cắt bớt nếu dài hơn 32 byte
        }
        return SecretKeySpec(paddedKeyBytes, "HmacSHA256") // Sử dụng HmacSHA256 cho HS256
    }
    // Cấu hình JwtEncoder sử dụng thuật toán HS256
    @Bean
    fun jwtEncoder(): JwtEncoder {
        // Tạo JWK từ secret key
        // JWK dùng để mã hóa và giải mã JWT
        val jwk = OctetSequenceKey.Builder(secretKeySpec()).build() 
        // jwSet dùng để chứa JWK, đóng gói jwk vào JWKSet
        val jwkSet = JWKSet(jwk) 
        // Tạo JWKSource từ JWKSet
        // JWKSource dùng để cung cấp JWK cho JwtEncoder
        val jwkSource = com.nimbusds.jose.jwk.source.JWKSource<SecurityContext> { selector, _ ->
            selector.select(jwkSet)
        }
        return NimbusJwtEncoder(jwkSource)
    }
    // Cấu hình JwtDecoder và JwtEncoder sử dụng thuật toán HS256
    @Bean
    fun jwtDecoder(): JwtDecoder {
        return NimbusJwtDecoder
            .withSecretKey(secretKeySpec())
            .macAlgorithm(MacAlgorithm.HS256)
            .build()
    }
}