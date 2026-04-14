package com.team8.backend.domain.user.service;

import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.repository.UserRepository;
import com.team8.backend.global.security.CustomUserDetails;
import com.team8.backend.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    @Transactional
    public User join(String email, String password, String nickname, String address) {
        userRepository.findByEmail(email)
                .ifPresent(_ -> {
                    // TODO: Exception 수정 필요
                    throw new RuntimeException("이미 사용 중인 이메일입니다.");
                });
        User user = User.create(email, passwordEncoder.encode(password), nickname, address);
        return userRepository.save(user);
    }

    public String login(String email, String password) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(email, password);
        Authentication authentication = authenticationManager.authenticate(authToken);
        return jwtProvider.generateToken(authentication);
    }

    @Transactional(readOnly = true)
    public User getMe(CustomUserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() ->
                        // TODO: Exception 수정 필요
                        new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
    }
}
