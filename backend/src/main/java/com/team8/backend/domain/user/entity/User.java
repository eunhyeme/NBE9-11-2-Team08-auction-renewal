package com.team8.backend.domain.user.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private String nickname;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private String address;

    @Column(name = "has_bluecheck", nullable = false)
    private Boolean hasBluecheck;

    protected User() {}

    public User(String email, UserRole role, String nickname, LocalDateTime createdAt, String address, Boolean hasBluecheck) {
        this.email = email;
        this.role = role;
        this.nickname = nickname;
        this.createdAt = createdAt;
        this.address = address;
        this.hasBluecheck = hasBluecheck;
    }

    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public UserRole getRole() { return role; }
    public String getNickname() { return nickname; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getAddress() { return address; }
    public Boolean getHasBluecheck() { return hasBluecheck; }
}
