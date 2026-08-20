package com.smartfinance.backend.usuario.service;

import com.smartfinance.backend.common.security.JwtService;
import com.smartfinance.backend.usuario.model.dto.AuthResponse;
import com.smartfinance.backend.usuario.model.dto.ChangePasswordRequest;
import com.smartfinance.backend.usuario.model.dto.LoginRequest;
import com.smartfinance.backend.usuario.model.dto.RegisterRequest;
import com.smartfinance.backend.usuario.model.dto.UpdateProfileRequest;
import com.smartfinance.backend.usuario.model.dto.UserResponse;
import com.smartfinance.backend.usuario.exception.EmailAlreadyExistsException;
import com.smartfinance.backend.usuario.exception.InvalidCredentialsException;
import com.smartfinance.backend.usuario.exception.InvalidRefreshTokenException;
import com.smartfinance.backend.common.exception.ResourceNotFoundException;
import com.smartfinance.backend.usuario.mapper.UserMapper;
import com.smartfinance.backend.ia.model.entity.AiMessageKind;
import com.smartfinance.backend.usuario.model.entity.User;
import com.smartfinance.backend.ia.repository.AiMessageRepository;
import com.smartfinance.backend.usuario.repository.UserRepository;
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
        // A fresh registration always gets a session cookie (rememberMe = false): the safer
        // default when the user has not explicitly opted into staying signed in.
        return buildAuthSession(createdUser, false);
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
        boolean rememberMe = Boolean.TRUE.equals(request.rememberMe());
        return buildAuthSession(user, rememberMe);
    }

    @Transactional
    public AuthSession refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException("Refresh token requerido");
        }

        RefreshTokenService.RotationResult result = refreshTokenService.rotate(refreshToken);
        return buildAuthSession(result.user(), result.refreshToken(), result.rememberMe());
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

    private AuthSession buildAuthSession(User user, boolean rememberMe) {
        String refreshToken = refreshTokenService.createForUser(user, rememberMe);
        return buildAuthSession(user, refreshToken, rememberMe);
    }

    private AuthSession buildAuthSession(User user, String refreshToken, boolean rememberMe) {
        String accessToken = jwtService.generateAccessToken(user);
        UserResponse userResponse = userMapper.toResponse(user);
        AuthResponse response = new AuthResponse(
                accessToken,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                userResponse,
                null
        );

        return new AuthSession(response, refreshToken, rememberMe);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
