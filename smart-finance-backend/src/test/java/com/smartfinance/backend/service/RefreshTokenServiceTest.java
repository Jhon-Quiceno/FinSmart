package com.smartfinance.backend.service;

import com.smartfinance.backend.model.RefreshToken;
import com.smartfinance.backend.model.User;
import com.smartfinance.backend.repository.RefreshTokenRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Focused on {@link RefreshTokenService#rotate}: {@code rememberMe} must survive rotation (the
 * persistent-vs-session cookie choice made at login should not silently flip back to session-only
 * on the very next refresh) and the presented token must always be revoked, whichever way
 * {@code rememberMe} goes.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void rotateShouldPropagateRememberMeTrueAndRevokeOldToken() {
        RefreshTokenService.RotationResult result = rotateStoredToken(true);

        Assertions.assertTrue(result.rememberMe());
    }

    @Test
    void rotateShouldPropagateRememberMeFalseAndRevokeOldToken() {
        RefreshTokenService.RotationResult result = rotateStoredToken(false);

        Assertions.assertFalse(result.rememberMe());
    }

    /**
     * Stubs a stored {@link RefreshToken} with the given {@code rememberMe}, rotates it, and
     * asserts the common invariants that must hold regardless of {@code rememberMe}: the old token
     * ends up revoked, and the new token propagates the same {@code rememberMe} both in the
     * returned {@link RefreshTokenService.RotationResult} and in the persisted new
     * {@link RefreshToken} row.
     */
    private RefreshTokenService.RotationResult rotateStoredToken(boolean rememberMe) {
        UUID tokenId = UUID.randomUUID();
        String oldRawToken = "old-refresh-token";
        User user = new User();
        user.setId(1L);

        RefreshToken stored = new RefreshToken();
        stored.setId(100L);
        stored.setTokenId(tokenId);
        stored.setUser(user);
        stored.setTokenHash(hashToken(oldRawToken));
        stored.setExpiresAt(Instant.now().plusSeconds(3600));
        stored.setRememberMe(rememberMe);

        Claims oldClaims = mock(Claims.class);
        when(oldClaims.getId()).thenReturn(tokenId.toString());
        when(jwtService.parseRefreshToken(oldRawToken)).thenReturn(oldClaims);
        when(refreshTokenRepository.findByTokenId(tokenId)).thenReturn(Optional.of(stored));

        String newRawToken = "new-refresh-token";
        when(jwtService.generateRefreshToken(eq(user), any(UUID.class))).thenReturn(newRawToken);
        Claims newClaims = mock(Claims.class);
        when(newClaims.getExpiration()).thenReturn(Date.from(Instant.now().plusSeconds(7200)));
        when(jwtService.parseRefreshToken(newRawToken)).thenReturn(newClaims);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenService.RotationResult result = refreshTokenService.rotate(oldRawToken);

        Assertions.assertEquals(newRawToken, result.refreshToken());
        Assertions.assertSame(user, result.user());
        Assertions.assertNotNull(stored.getRevokedAt(), "the presented token must be revoked on rotation");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(captor.capture());
        RefreshToken savedOld = captor.getAllValues().get(0);
        RefreshToken savedNew = captor.getAllValues().get(1);
        Assertions.assertNotNull(savedOld.getRevokedAt());
        Assertions.assertEquals(rememberMe, savedNew.isRememberMe());

        return result;
    }

    /** Mirrors {@code RefreshTokenService#hashToken} so the stored token hash matches at check time. */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
