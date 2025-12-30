package lc._th5.gym_BE.config

import lc._th5.gym_BE.model.user.Role
import lc._th5.gym_BE.model.user.User
import lc._th5.gym_BE.repository.UserRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class AdminInitializer {

    @Bean
    fun initAdmin(
        userRepository: UserRepository,
        passwordEncoder: PasswordEncoder
    ): CommandLineRunner {
        return CommandLineRunner {
            // Tạo Admin User
            if (userRepository.findByUsername("admin") == null) {
                val admin = User(
                    username = "admin",
                    password = passwordEncoder.encode("admin123"),
                    email = "admin@gym.com",
                    fullName = "Administrator",
                    roles = mutableSetOf(Role.ADMIN, Role.USER),
                    isVerified = true
                )
                userRepository.save(admin)
                println("ADMIN USER CREATED: username=admin, password=admin123")
            }
        }
    }
}
