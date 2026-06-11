package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.auth.LoginRequest;
import com.smartfinance.backend.dto.auth.RegisterRequest;
import com.smartfinance.backend.dto.auth.UserResponse;
import com.smartfinance.backend.exception.EmailAlreadyExistsException;
import com.smartfinance.backend.exception.InvalidCredentialsException;
import com.smartfinance.backend.exception.InvalidRefreshTokenException;
import com.smartfinance.backend.mapper.UserMapper;
import com.smartfinance.backend.model.User;
import com.smartfinance.backend.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void registerShouldCreateUserAndReturnAuthResponse() {
        RegisterRequest request = new RegisterRequest("Ana", " ANA@MAIL.COM ", "secret123");
        when(userRepository.existsByEmailIgnoreCase("ana@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            Assertions.assertTrue(user.isActive());
            user.setId(10L);
            return user;
        });
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(refreshTokenService.createForUser(any(User.class))).thenReturn("refresh-token");
        when(userMapper.toResponse(any(User.class))).thenReturn(new UserResponse(10L, "Ana", "ana@mail.com"));

        AuthSession session = userService.register(request);

        Assertions.assertEquals("access-token", session.response().accessToken());
        Assertions.assertEquals("Bearer", session.response().tokenType());
        Assertions.assertEquals(900L, session.response().expiresIn());
        Assertions.assertEquals(10L, session.response().user().id());
        Assertions.assertEquals("refresh-token", session.refreshToken());
    }

    @Test
    void registerShouldFailWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("Ana", "ana@mail.com", "secret123");
        when(userRepository.existsByEmailIgnoreCase("ana@mail.com")).thenReturn(true);

        Assertions.assertThrows(EmailAlreadyExistsException.class, () -> userService.register(request));
    }

    @Test
    void loginShouldReturnAuthResponseForValidCredentials() {
        User user = new User();
        user.setId(7L);
        user.setName("John");
        user.setEmail("john@mail.com");
        user.setPasswordHash("hashed");
        user.setActive(true);
        when(userRepository.findByEmailIgnoreCase("john@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(refreshTokenService.createForUser(any(User.class))).thenReturn("refresh-token");
        when(userMapper.toResponse(any(User.class))).thenReturn(new UserResponse(7L, "John", "john@mail.com"));

        AuthSession session = userService.login(new LoginRequest("JOHN@mail.com", "secret123"));

        Assertions.assertEquals("access-token", session.response().accessToken());
        Assertions.assertEquals(7L, session.response().user().id());
        Assertions.assertEquals("refresh-token", session.refreshToken());
        Assertions.assertNotNull(user.getLastLoginAt());
    }

    @Test
    void loginShouldFailWhenPasswordIsInvalid() {
        User user = new User();
        user.setId(7L);
        user.setName("John");
        user.setEmail("john@mail.com");
        user.setPasswordHash("hashed");
        user.setActive(true);
        when(userRepository.findByEmailIgnoreCase("john@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad-pass", "hashed")).thenReturn(false);

        Assertions.assertThrows(InvalidCredentialsException.class,
                () -> userService.login(new LoginRequest("john@mail.com", "bad-pass")));
    }

    @Test
    void loginShouldFailWhenUserIsInactive() {
        User user = new User();
        user.setId(9L);
        user.setName("Inactive");
        user.setEmail("inactive@mail.com");
        user.setPasswordHash("hashed");
        user.setActive(false);
        when(userRepository.findByEmailIgnoreCase("inactive@mail.com")).thenReturn(Optional.of(user));

        Assertions.assertThrows(InvalidCredentialsException.class,
                () -> userService.login(new LoginRequest("inactive@mail.com", "secret123")));
    }

    @Test
    void refreshShouldFailWhenTokenIsMissing() {
        Assertions.assertThrows(InvalidRefreshTokenException.class, () -> userService.refresh(" "));
    }
}
