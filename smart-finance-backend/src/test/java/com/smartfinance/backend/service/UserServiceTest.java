package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.auth.AuthResponse;
import com.smartfinance.backend.dto.auth.LoginRequest;
import com.smartfinance.backend.dto.auth.RegisterRequest;
import com.smartfinance.backend.exception.EmailAlreadyExistsException;
import com.smartfinance.backend.exception.InvalidCredentialsException;
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

    @InjectMocks
    private UserService userService;

    @Test
    void registerShouldCreateUserAndReturnAuthResponse() {
        RegisterRequest request = new RegisterRequest("Ana", " ANA@MAIL.COM ", "secret123");
        when(userRepository.existsByEmailIgnoreCase("ana@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });

        AuthResponse response = userService.register(request);

        Assertions.assertNotNull(response.token());
        Assertions.assertTrue(response.token().startsWith("stub-"));
        Assertions.assertEquals(10L, response.user().id());
        Assertions.assertEquals("Ana", response.user().name());
        Assertions.assertEquals("ana@mail.com", response.user().email());
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
        when(userRepository.findByEmailIgnoreCase("john@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hashed")).thenReturn(true);

        AuthResponse response = userService.login(new LoginRequest("JOHN@mail.com", "secret123"));

        Assertions.assertNotNull(response.token());
        Assertions.assertEquals(7L, response.user().id());
        Assertions.assertEquals("john@mail.com", response.user().email());
    }

    @Test
    void loginShouldFailWhenPasswordIsInvalid() {
        User user = new User();
        user.setId(7L);
        user.setName("John");
        user.setEmail("john@mail.com");
        user.setPasswordHash("hashed");
        when(userRepository.findByEmailIgnoreCase("john@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad-pass", "hashed")).thenReturn(false);

        Assertions.assertThrows(InvalidCredentialsException.class,
                () -> userService.login(new LoginRequest("john@mail.com", "bad-pass")));
    }
}
