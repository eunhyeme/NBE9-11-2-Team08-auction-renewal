package com.team8.backend.global.security;

import com.team8.backend.domain.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long expireMillis;

    public JwtProvider(
            @Value("${custom.jwt.secretPattern}") String secretPattern,
            @Value("${custom.jwt.expireSeconds}") long expireSeconds) {
        this.secretKey = Keys.hmacShaKeyFor(secretPattern.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireSeconds * 1000L;
    }

    public String generateToken(Authentication authentication) {
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(principal.getUserId()))
                .claim("email", principal.getUsername())
                .claim("role", principal.getAuthorities().iterator().next().getAuthority())
                .claim("nickname", principal.getNickname())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expireMillis))
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = Long.parseLong(claims.getSubject());
        String email = claims.get("email", String.class);
        UserRole role = UserRole.valueOf(claims.get("role", String.class));
        String nickname = claims.get("nickname", String.class);

        CustomUserDetails userDetails = new CustomUserDetails(userId, email, role, nickname);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
