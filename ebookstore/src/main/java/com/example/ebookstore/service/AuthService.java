package com.example.ebookstore.service;

import com.example.ebookstore.config.JwtUtil;
import com.example.ebookstore.dto.AuthResponse;
import com.example.ebookstore.dto.LoginRequest;
import com.example.ebookstore.dto.RegisterRequest;
import com.example.ebookstore.entity.Cart;
import com.example.ebookstore.entity.User;
import com.example.ebookstore.exception.ConflictException;
import com.example.ebookstore.repository.CartRepository;
import com.example.ebookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository        userRepository;
    private final CartRepository        cartRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtUtil               jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ConflictException("Email already registered: " + request.getEmail());
        }

        User user = userRepository.save(User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .giftPointsBalance(0)
                .build());

        // Create an empty cart for the new user
        cartRepository.save(Cart.builder().user(user).build());

        log.info("Registered new user id={}", user.getId());
        String token = jwtUtil.generateToken(user.getEmail());
        return buildResponse(token, user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        String token = jwtUtil.generateToken(user.getEmail());
        log.info("User id={} logged in", user.getId());
        return buildResponse(token, user);
    }

    private AuthResponse buildResponse(String token, User user) {
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
