package me.kitkas1412.ticketbooking.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.EnumSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email")
)
public class User extends BaseEntity {

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /**
     * BCrypt hash, không bao giờ là plaintext. Bị loại khỏi toString() để
     * không rò rỉ ra log.
     */
    @ToString.Exclude
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id", nullable = false),
            uniqueConstraints = @UniqueConstraint(name = "uk_user_roles", columnNames = {"user_id", "role"})
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private Set<Role> roles = EnumSet.of(Role.USER);

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    /**
     * Chuẩn hoá về chữ thường: unique constraint của Postgres phân biệt hoa
     * thường, nếu không normalize thì "A@x.com" và "a@x.com" sẽ là hai tài khoản.
     * Đặt ở callback thay vì setter để phủ cả đường ghi qua @SuperBuilder —
     * builder gán thẳng field, không đi qua setter.
     */
    @PrePersist
    @PreUpdate
    private void normalizeEmail() {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public enum UserStatus {
        ACTIVE, LOCKED, DISABLED
    }
}
