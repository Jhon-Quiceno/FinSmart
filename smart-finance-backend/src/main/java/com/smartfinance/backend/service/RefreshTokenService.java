package com.smartfinance.backend.service;

import com.smartfinance.backend.exception.InvalidRefreshTokenException;
import com.smartfinance.backend.model.RefreshToken;
import com.smartfinance.backend.model.User;
import com.smartfinance.backend.repository.RefreshTokenRepository;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtService jwtService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public String createForUser(User user) {
        UUID tokenId = UUID.randomUUID();
        String rawToken = jwtService.generateRefreshToken(user, tokenId);
        Claims claims = jwtService.parseRefreshToken(rawToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenId(tokenId);
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setCreatedAt(Instant.now());
        refreshToken.setExpiresAt(claims.getExpiration().toInstant());
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Transactional
    public RotationResult rotate(String rawToken) {
        RefreshToken stored = resolveActiveToken(rawToken);
        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

        String newRefreshToken = createForUser(stored.getUser());
        return new RotationResult(stored.getUser(), newRefreshToken);
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        try {
            RefreshToken refreshToken = resolveActiveToken(rawToken);
            refreshToken.setRevokedAt(Instant.now());
            refreshTokenRepository.save(refreshToken);
        } catch (InvalidRefreshTokenException ignored) {
            // Logout should stay idempotent.
        }
    }

    /**
     * Revokes every active refresh token belonging to {@code userId}, so a password change
     * invalidates other sessions instead of leaving stolen/lingering refresh tokens usable.
     */
    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllActiveForUser(userId, Instant.now());
    }

    private RefreshToken resolveActiveToken(String rawToken) {
        Claims claims = jwtService.parseRefreshToken(rawToken);
        String tokenIdRaw = claims.getId();
        if (tokenIdRaw == null || tokenIdRaw.isBlank()) {
            throw new InvalidRefreshTokenException("Refresh token inválido");
        }

        UUID tokenId = UUID.fromString(tokenIdRaw);
        Optional<RefreshToken> optionalToken = refreshTokenRepository.findByTokenId(tokenId);
        RefreshToken refreshToken = optionalToken.orElseThrow(
                () -> new InvalidRefreshTokenException("Refresh token inválido")
        );

        if (refreshToken.getRevokedAt() != null) {
            throw new InvalidRefreshTokenException("Refresh token revocado");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException("Refresh token expirado");
        }

        String hashedRawToken = hashToken(rawToken);
        if (!hashedRawToken.equals(refreshToken.getTokenHash())) {
            throw new InvalidRefreshTokenException("Refresh token inválido");
        }

        return refreshToken;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("No se pudo inicializar el algoritmo de hash para refresh token", ex);
        }
    }

    public record RotationResult(
            User user,
            String refreshToken
    ) {
    }
}
