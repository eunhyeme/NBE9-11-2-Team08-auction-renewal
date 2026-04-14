package com.team8.backend.global.security;

import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.entity.UserRole;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    @Getter
    private final Long userId;
    private final String email;
    private final String password;
    private final UserRole role;
    @Getter
    private final String nickname;

    public CustomUserDetails(User user) {
        this.userId = user.getUserId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.role = user.getRole();
        this.nickname = user.getNickname();
    }

    public CustomUserDetails(Long userId, String email, UserRole role, String nickname) {
        this.userId = userId;
        this.email = email;
        this.password = null;
        this.role = role;
        this.nickname = nickname;
    }

    @Override
    @NullMarked
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    @NullMarked
    public String getUsername() {
        return email;
    }

}
