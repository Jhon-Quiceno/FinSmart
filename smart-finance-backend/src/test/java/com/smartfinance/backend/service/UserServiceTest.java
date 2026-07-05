package com.smartfinance.backend.service;

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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    @Mock
    private AiMessageRepository aiMessageRepository;

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
        verify(aiMessageRepository).deleteByUserIdAndKind(7L, AiMessageKind.CHAT);
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
        verify(aiMessageRepository, never()).deleteByUserIdAndKind(any(), any());
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

    @Test
    void updateProfileShouldUpdateNameAndEmailWhenEmailIsNotTaken() {
        User user = new User();
        user.setId(1L);
        user.setName("Old Name");
        user.setEmail("old@mail.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase("new@mail.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(new UserResponse(1L, "New Name", "new@mail.com"));

        UserResponse response = userService.updateProfile(1L, new UpdateProfileRequest("New Name", "NEW@mail.com"));

        Assertions.assertEquals("New Name", response.name());
        Assertions.assertEquals("new@mail.com", response.email());
        Assertions.assertEquals("New Name", user.getName());
        Assertions.assertEquals("new@mail.com", user.getEmail());
    }

    @Test
    void updateProfileShouldAllowKeepingTheSameEmail() {
        User user = new User();
        user.setId(1L);
        user.setName("Old Name");
        user.setEmail("same@mail.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(new UserResponse(1L, "New Name", "same@mail.com"));

        userService.updateProfile(1L, new UpdateProfileRequest("New Name", "same@mail.com"));

        verify(userRepository, never()).existsByEmailIgnoreCase(any());
    }

    @Test
    void updateProfileShouldFailWhenEmailBelongsToAnotherUser() {
        User user = new User();
        user.setId(1L);
        user.setName("Old Name");
        user.setEmail("old@mail.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase("taken@mail.com")).thenReturn(true);

        Assertions.assertThrows(EmailAlreadyExistsException.class,
                () -> userService.updateProfile(1L, new UpdateProfileRequest("New Name", "taken@mail.com")));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfileShouldFailWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class,
                () -> userService.updateProfile(99L, new UpdateProfileRequest("Name", "email@mail.com")));
    }

    @Test
    void updateProfileShouldMapDataIntegrityViolationToEmailAlreadyExistsException() {
        User user = new User();
        user.setId(1L);
        user.setName("Old Name");
        user.setEmail("old@mail.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase("new@mail.com")).thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("uk_users_email"));

        Assertions.assertThrows(EmailAlreadyExistsException.class,
                () -> userService.updateProfile(1L, new UpdateProfileRequest("New Name", "new@mail.com")));
    }

    @Test
    void changePasswordShouldUpdateHashWhenCurrentPasswordMatches() {
        User user = new User();
        user.setId(1L);
        user.setPasswordHash("old-hash");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("new-hash");

        userService.changePassword(1L, new ChangePasswordRequest("oldPassword", "newPassword"));

        Assertions.assertEquals("new-hash", user.getPasswordHash());
        verify(userRepository).save(user);
        verify(refreshTokenService).revokeAllForUser(1L);
    }

    @Test
    void changePasswordShouldFailWhenCurrentPasswordDoesNotMatch() {
        User user = new User();
        user.setId(1L);
        user.setPasswordHash("old-hash");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "old-hash")).thenReturn(false);

        Assertions.assertThrows(InvalidCredentialsException.class,
                () -> userService.changePassword(1L, new ChangePasswordRequest("wrongPassword", "newPassword")));
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAllForUser(any());
    }

    @Test
    void changePasswordShouldFailWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class,
                () -> userService.changePassword(99L, new ChangePasswordRequest("oldPassword", "newPassword")));
    }
}
