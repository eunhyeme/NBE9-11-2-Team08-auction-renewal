package com.team8.backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@EntityListeners(AuditingEntityListener.class)
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
    private String password;

    @Column(nullable = false)
    private String nickname;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private String address;

    @Column(name = "has_bluecheck", nullable = false)
    private Boolean hasBluecheck;

    protected User() {}

    private User(String email, String password, UserRole role, String nickname, String address, Boolean hasBluecheck) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.nickname = nickname;
        this.address = address;
        this.hasBluecheck = hasBluecheck;
    }

    public static User create(String email, String password, String nickname, String address) {
        return new User(email, password, UserRole.ROLE_USER, nickname, address, false);
    }

    public static User createAdmin(String email, String password, String nickname, String address) {
        return new User(email, password, UserRole.ROLE_ADMIN, nickname, address, true);
    }

    public void grantBluecheck() {
        this.hasBluecheck = true;
    }
}
