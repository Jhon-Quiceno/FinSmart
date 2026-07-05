package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.auth.AuthResponse;
import com.smartfinance.backend.dto.auth.ChangePasswordRequest;
import com.smartfinance.backend.dto.auth.LoginRequest;
import com.smartfinance.backend.dto.auth.RegisterRequest;
import com.smartfinance.backend.dto.auth.UpdateProfileRequest;
import com.smartfinance.backend.dto.auth.UserResponse;
import com.smartfinance.backend.exception.EmailAlreadyExistsException;
import com.smartfinance.backend.exception.InvalidCredentialsException;
import com.smartfinance.backend.exception.InvalidRefreshTokenException;
import com.smartfinance.backend.exception.ResourceNotFoundException;
import com.smartfinance.backend.mapper.UserMapper;
import com.smartfinance.backend.model.AiMessageKind;
import com.smartfinance.backend.model.User;
import com.smartfinance.backend.repository.AiMessageRepository;
import com.smartfinance.backend.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final AiMessageRepository aiMessageRepository;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            UserMapper userMapper,
            AiMessageRepository aiMessageRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userMapper = userMapper;
        this.aiMessageRepository = aiMessageRepository;
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

    /**
     * Authenticates the user and, on success, clears their AI assistant chat history
     * ({@link AiMessageKind#CHAT} rows only — {@link AiMessageKind#INSIGHT} rows persist across
     * logins) so every new session starts the assistant with a blank conversation.
     */
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
        aiMessageRepository.deleteByUserIdAndKind(user.getId(), AiMessageKind.CHAT);
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

    /**
     * Updates the current user's name and email, rejecting the change if the new email is
     * already taken by another user.
     *
     * @param userId  identifier of the user to update
     * @param request new name/email
     * @return the updated user, mapped to {@link UserResponse}
     * @throws ResourceNotFoundException     if no user exists with {@code userId}
     * @throws EmailAlreadyExistsException   if another user already owns {@code request.email()}
     */
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        String normalizedEmail = normalizeEmail(request.email());
        boolean emailChanged = !normalizedEmail.equalsIgnoreCase(user.getEmail());
        if (emailChanged && userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException("Ya existe un usuario registrado con este correo electrónico");
        }

        user.setName(request.name().trim());
        user.setEmail(normalizedEmail);

        try {
            User updatedUser = userRepository.save(user);
            return userMapper.toResponse(updatedUser);
        } catch (DataIntegrityViolationException ex) {
            // Narrow TOCTOU window: another request took the same email between the check above
            // and this save. The DB's unique constraint is the real backstop; map it to the same
            // 409 the pre-check would have produced instead of letting it surface as a 500.
            throw new EmailAlreadyExistsException("Ya existe un usuario registrado con este correo electrónico");
        }
    }

    /**
     * Changes the current user's password after verifying {@code request.currentPassword()}
     * against the stored hash, then revokes every other active refresh token for this user so a
     * stolen or lingering session can't keep using the old credentials.
     *
     * @param userId  identifier of the user changing their password
     * @param request current and new password
     * @throws ResourceNotFoundException    if no user exists with {@code userId}
     * @throws InvalidCredentialsException  if {@code request.currentPassword()} does not match
     *                                       the stored hash
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("La contraseña actual no es correcta");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(userId);
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
