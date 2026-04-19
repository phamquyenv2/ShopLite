package com.quyen.shoplite.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    private Integer version;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(length = 20)
    private String phone;

    /**
     * Thay từ RoleEnum → Role entity để hỗ trợ permission động.
     * Một User chỉ có MỘT role.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
