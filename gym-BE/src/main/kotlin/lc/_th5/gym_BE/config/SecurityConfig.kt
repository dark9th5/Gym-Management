package lc._th5.gym_BE.config

import lc._th5.gym_BE.repository.UserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableMethodSecurity
@EnableScheduling
class SecurityConfig(
    private val userRepository: UserRepository
) {

    // Mã hóa mật khẩu 
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12) 
    
    // Cung cấp UserDetailsService để load user từ database
    @Bean
    fun userDetailsService(): UserDetailsService = UserDetailsService { identifier ->
        val user = if (identifier.contains("@")) {
            userRepository.findByEmail(identifier)
        } else {
            userRepository.findByUsername(identifier)
        } ?: throw org.springframework.security.core.userdetails.UsernameNotFoundException("User not found")
        org.springframework.security.core.userdetails.User
            .withUsername(user.email)
            .password(user.password)
            .authorities(user.roles.map { SimpleGrantedAuthority("ROLE_${it.name}") })
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(false)
            .build()
    }
    // Cung cấp AuthenticationManager để xử lý xác thực
    @Bean
    fun authenticationManager(
        userDetailsService: UserDetailsService,
        passwordEncoder: PasswordEncoder
    ): AuthenticationManager {
        val provider = DaoAuthenticationProvider(userDetailsService).apply {
            setPasswordEncoder(passwordEncoder)
        }
        return ProviderManager(provider)
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() } // Disable CSRF cho API
            .cors(withDefaults()) // Enable CORS với cấu hình mặc định
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) } // Stateless session (JWT)
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/api/auth/**",
                    "/api/users/register"
                ).permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->// Cấu hình Resource Server để sử dụng JWT
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                } // Sử dụng  jwtDecoder để giải mã và xác thực JWT
            }
        return http.build()
    }

    private fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val grantedAuthoritiesConverter = JwtGrantedAuthoritiesConverter().apply {
            setAuthorityPrefix("") // Keep ROLE_ prefix from token claims
            setAuthoritiesClaimName("scope")
        }

        return JwtAuthenticationConverter().apply {
            setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter)
        }
    }
}