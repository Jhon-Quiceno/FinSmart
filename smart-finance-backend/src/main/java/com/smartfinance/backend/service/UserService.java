package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.auth.AuthResponse;
import com.smartfinance.backend.dto.auth.LoginRequest;
import com.smartfinance.backend.dto.auth.RegisterRequest;
import com.smartfinance.backend.dto.auth.UserResponse;
import com.smartfinance.backend.exception.EmailAlreadyExistsException;
import com.smartfinance.backend.exception.InvalidCredentialsException;
import com.smartfinance.backend.exception.InvalidRefreshTokenException;
import com.smartfinance.backend.mapper.UserMapper;
import com.smartfinance.backend.model.User;
import com.smartfinance.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userMapper = userMapper;
    }

    @Transactional
    public AuthSession register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException("Ya existe un usuario registrado con este correo electrónico");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setActive(true);

        User createdUser = userRepository.save(user);
        return buildAuthSession(createdUser);
    }

    @Transactional
    public AuthSession login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        User user = userRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Correo o contraseña inválidos"));

        if (!user.isActive()) {
            throw new InvalidCredentialsException("Correo o contraseña inválidos");
        }

        boolean isPasswordValid = passwordEncoder.matches(request.password(), user.getPasswordHash());
        if (!isPasswordValid) {
            throw new InvalidCredentialsException("Correo o contraseña inválidos");
        }

        user.setLastLoginAt(java.time.Instant.now());
        return buildAuthSession(user);
    }

    @Transactional
    public AuthSession refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException("Refresh token requerido");
        }

        RefreshTokenService.RotationResult result = refreshTokenService.rotate(refreshToken);
        return buildAuthSession(result.user(), result.refreshToken());
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private AuthSession buildAuthSession(User user) {
        String refreshToken = refreshTokenService.createForUser(user);
        return buildAuthSession(user, refreshToken);
    }

    private AuthSession buildAuthSession(User user, String refreshToken) {
        String accessToken = jwtService.generateAccessToken(user);
        UserResponse userResponse = userMapper.toResponse(user);
        AuthResponse response = new AuthResponse(
                accessToken,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                userResponse
        );

        return new AuthSession(response, refreshToken);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
