package lc._th5.gym_BE.model.user

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "users", indexes = [Index(name = "idx_user_email", columnList = "email")])
data class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val username: String,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    val password: String,

    @Column(name = "full_name")
    val fullName: String? = null,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = [JoinColumn(name = "user_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    var roles: MutableSet<Role> = mutableSetOf(Role.USER),

    @Column(name = "is_verified", nullable = false)
    var isVerified: Boolean = false,

    @Column(name = "created_at")
    @CreationTimestamp
    val createdAt: LocalDateTime? = null,
    
    // ==================== 2FA TOTP Fields ====================
    
    /**
     * Secret key cho TOTP (mã hóa Base32)
     * Được tạo khi user bật 2FA và dùng để xác thực mã từ Google Authenticator
     */
    @Column(name = "totp_secret")
    var totpSecret: String? = null,
    
    /**
     * Trạng thái 2FA đã được bật hay chưa
     */
    @Column(name = "is_2fa_enabled", nullable = false)
    var is2faEnabled: Boolean = false,
    
    /**
     * Backup codes cho trường hợp mất điện thoại (JSON array)
     * Mỗi code chỉ dùng được 1 lần
     */
    @Column(name = "backup_codes", length = 500)
    var backupCodes: String? = null
)