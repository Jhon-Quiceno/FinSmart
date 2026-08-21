package com.smartfinance.backend.usuario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinance.backend.common.config.JwtProperties;
import com.smartfinance.backend.common.config.SecurityConfig;
import com.smartfinance.backend.usuario.model.dto.AuthResponse;
import com.smartfinance.backend.usuario.model.dto.ChangePasswordRequest;
import com.smartfinance.backend.usuario.model.dto.LoginRequest;
import com.smartfinance.backend.usuario.model.dto.RegisterRequest;
import com.smartfinance.backend.usuario.model.dto.RefreshRequest;
import com.smartfinance.backend.usuario.model.dto.UpdateProfileRequest;
import com.smartfinance.backend.usuario.model.dto.UserResponse;
import com.smartfinance.backend.usuario.exception.EmailAlreadyExistsException;
import com.smartfinance.backend.usuario.exception.InvalidCredentialsException;
import com.smartfinance.backend.usuario.exception.InvalidRefreshTokenException;
import com.smartfinance.backend.usuario.repository.UserRepository;
import com.smartfinance.backend.usuario.service.AuthSession;
import com.smartfinance.backend.common.security.JwtService;
import com.smartfinance.backend.usuario.service.UserService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtProperties jwtProperties;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final String AUTH_HEADER = "Bearer test-token";

    @BeforeEach
    void setUp() {
        Claims mockClaims = org.mockito.Mockito.mock(Claims.class);
        when(mockClaims.getSubject()).thenReturn("1");
        when(jwtService.parseAccessToken(any())).thenReturn(mockClaims);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(jwtProperties.refreshCookieName()).thenReturn("refresh_token");
        when(jwtProperties.refreshCookieSecure()).thenReturn(true);
        when(jwtProperties.refreshCookieSameSite()).thenReturn("Strict");
        when(jwtProperties.refreshExpirationMs()).thenReturn(604_800_000L);
    }

    @Test
    void loginSetsPersistentCookieWithMaxAgeWhenRememberMeIsTrue() throws Exception {
        LoginRequest request = new LoginRequest("jane@example.com", "secret123", true);
        AuthSession session = new AuthSession(
                new AuthResponse("access-token", "Bearer", 900L, new UserResponse(1L, "Jane", "jane@example.com"), null),
                "refresh-token",
                true
        );
        when(userService.login(any(LoginRequest.class))).thenReturn(session);

        mockMvc.perform(post("/api/users/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
                    org.assertj.core.api.Assertions.assertThat(setCookie).contains("Max-Age=604800");
                });
    }

    @Test
    void loginSetsSessionCookieWithoutMaxAgeWhenRememberMeIsFalse() throws Exception {
        LoginRequest request = new LoginRequest("jane@example.com", "secret123", false);
        AuthSession session = new AuthSession(
                new AuthResponse("access-token", "Bearer", 900L, new UserResponse(1L, "Jane", "jane@example.com"), null),
                "refresh-token",
                false
        );
        when(userService.login(any(LoginRequest.class))).thenReturn(session);

        mockMvc.perform(post("/api/users/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
                    org.assertj.core.api.Assertions.assertThat(setCookie).doesNotContain("Max-Age");
                });
    }

    @Test
    void loginReturnsRefreshTokenInBodyAndNoCookieForMobileClient() throws Exception {
        LoginRequest request = new LoginRequest("jane@example.com", "secret123", false);
        AuthSession session = new AuthSession(
                new AuthResponse("access-token", "Bearer", 900L, new UserResponse(1L, "Jane", "jane@example.com"), null),
                "refresh-token",
                false
        );
        when(userService.login(any(LoginRequest.class))).thenReturn(session);

        mockMvc.perform(post("/api/users/login")
                        .header("X-Client", "mobile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(result ->
                        org.assertj.core.api.Assertions.assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull());
    }

    @Test
    void loginOmitsRefreshTokenFromBodyForWebClient() throws Exception {
        LoginRequest request = new LoginRequest("jane@example.com", "secret123", false);
        AuthSession session = new AuthSession(
                new AuthResponse("access-token", "Bearer", 900L, new UserResponse(1L, "Jane", "jane@example.com"), null),
                "refresh-token",
                false
        );
        when(userService.login(any(LoginRequest.class))).thenReturn(session);

        mockMvc.perform(post("/api/users/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(result ->
                        org.assertj.core.api.Assertions.assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNotNull());
    }

    @Test
    void registerReturnsRefreshTokenInBodyAndNoCookieForMobileClient() throws Exception {
        RegisterRequest request = new RegisterRequest("Jane", "jane@example.com", "secret123");
        AuthSession session = new AuthSession(
                new AuthResponse("access-token", "Bearer", 900L, new UserResponse(1L, "Jane", "jane@example.com"), null),
                "refresh-token",
                false
        );
        when(userService.register(any(RegisterRequest.class))).thenReturn(session);

        mockMvc.perform(post("/api/users/register")
                        .header("X-Client", "mobile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(result ->
                        org.assertj.core.api.Assertions.assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull());
    }

    @Test
    void registerOmitsRefreshTokenFromBodyForWebClient() throws Exception {
        RegisterRequest request = new RegisterRequest("Jane", "jane@example.com", "secret123");
        AuthSession session = new AuthSession(
                new AuthResponse("access-token", "Bearer", 900L, new UserResponse(1L, "Jane", "jane@example.com"), null),
                "refresh-token",
                false
        );
        when(userService.register(any(RegisterRequest.class))).thenReturn(session);

        mockMvc.perform(post("/api/users/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(result ->
                        org.assertj.core.api.Assertions.assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNotNull());
    }

    @Test
    void refreshReturnsRefreshTokenInBodyAndNoCookieForMobileClient() throws Exception {
        AuthSession session = new AuthSession(
                new AuthResponse("access-token", "Bearer", 900L, new UserResponse(1L, "Jane", "jane@example.com"), null),
                "new-refresh-token",
                false
        );
        when(userService.refresh(any())).thenReturn(session);

        mockMvc.perform(post("/api/users/refresh")
                        .header("X-Client", "mobile")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "old-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andExpect(result ->
                        org.assertj.core.api.Assertions.assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull());
    }

    @Test
    void refreshOmitsRefreshTokenFromBodyForWebClient() throws Exception {
        AuthSession session = new AuthSession(
                new AuthResponse("access-token", "Bearer", 900L, new UserResponse(1L, "Jane", "jane@example.com"), null),
                "new-refresh-token",
                false
        );
        when(userService.refresh(any())).thenReturn(session);

        mockMvc.perform(post("/api/users/refresh")
                        .with(csrf())
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "old-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(result ->
                        org.assertj.core.api.Assertions.assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNotNull());
    }

    @Test
    void refreshWithMobileHeaderSkipsCsrfProtection() throws Exception {
        AuthSession session = new AuthSession(
                new AuthResponse("access-token", "Bearer", 900L, new UserResponse(1L, "Jane", "jane@example.com"), null),
                "new-refresh-token",
                false
        );
        when(userService.refresh(any())).thenReturn(session);

        // No .with(csrf()) and no X-XSRF-TOKEN header: a mobile client should still succeed.
        mockMvc.perform(post("/api/users/refresh")
                        .header("X-Client", "mobile")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "old-refresh-token")))
                .andExpect(status().isOk());
    }

    @Test
    void refreshWithoutMobileHeaderStillRequiresCsrf() throws Exception {
        // No .with(csrf()) and no X-Client header: a browser client must still be rejected by CSRF.
        mockMvc.perform(post("/api/users/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "old-refresh-token")))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshUsesRefreshTokenFromBodyWhenMobileClientHasNoCookie() throws Exception {
        AuthSession session = new AuthSession(
                new AuthResponse("access-token", "Bearer", 900L, new UserResponse(1L, "Jane", "jane@example.com"), null),
                "new-refresh-token",
                false
        );
        when(userService.refresh("stored-token")).thenReturn(session);

        mockMvc.perform(post("/api/users/refresh")
                        .header("X-Client", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("stored-token"))))
                .andExpect(status().isOk());

        verify(userService).refresh("stored-token");
    }

    @Test
    void refreshPrefersCookieOverBodyWhenBothArePresent() throws Exception {
        AuthSession session = new AuthSession(
                new AuthResponse("access-token", "Bearer", 900L, new UserResponse(1L, "Jane", "jane@example.com"), null),
                "new-refresh-token",
                false
        );
        when(userService.refresh("cookie-token")).thenReturn(session);

        mockMvc.perform(post("/api/users/refresh")
                        .header("X-Client", "mobile")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "cookie-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("body-token"))))
                .andExpect(status().isOk());

        verify(userService).refresh("cookie-token");
    }

    @Test
    void refreshStillWorksForBrowserWithJsonContentTypeAndEmptyBody() throws Exception {
        AuthSession session = new AuthSession(
                new AuthResponse("access-token", "Bearer", 900L, new UserResponse(1L, "Jane", "jane@example.com"), null),
                "new-refresh-token",
                false
        );
        when(userService.refresh("cookie-token")).thenReturn(session);

        mockMvc.perform(post("/api/users/refresh")
                        .with(csrf())
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "cookie-token"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(userService).refresh("cookie-token");
    }

    @Test
    void refreshStillWorksForBrowserWithoutContentTypeAndWithoutBody() throws Exception {
        AuthSession session = new AuthSession(
                new AuthResponse("access-token", "Bearer", 900L, new UserResponse(1L, "Jane", "jane@example.com"), null),
                "new-refresh-token",
                false
        );
        when(userService.refresh("cookie-token")).thenReturn(session);

        mockMvc.perform(post("/api/users/refresh")
                        .with(csrf())
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "cookie-token")))
                .andExpect(status().isOk());

        verify(userService).refresh("cookie-token");
    }

    @Test
    void refreshReturns401WhenNeitherCookieNorBodyCarryToken() throws Exception {
        when(userService.refresh(isNull()))
                .thenThrow(new InvalidRefreshTokenException("Refresh token requerido"));

        mockMvc.perform(post("/api/users/refresh")
                        .header("X-Client", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithMalformedJsonBodyReturns400() throws Exception {
        mockMvc.perform(post("/api/users/refresh")
                        .header("X-Client", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logoutUsesRefreshTokenFromBodyWhenNoCookie() throws Exception {
        mockMvc.perform(post("/api/users/logout")
                        .header("X-Client", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("stored-token"))))
                .andExpect(status().isNoContent());

        verify(userService).logout("stored-token");
    }

    @Test
    void logoutStillClearsCookieEvenForMobileClient() throws Exception {
        mockMvc.perform(post("/api/users/logout")
                        .header("X-Client", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("stored-token"))))
                .andExpect(status().isNoContent())
                .andExpect(result -> {
                    String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
                    org.assertj.core.api.Assertions.assertThat(setCookie).contains("Max-Age=0");
                });
    }

    @Test
    void updateProfileReturns200WithUpdatedUserWhenValid() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Jane Doe", "jane@example.com");
        UserResponse response = new UserResponse(1L, "Jane Doe", "jane@example.com");
        when(userService.updateProfile(eq(1L), any(UpdateProfileRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/users/profile")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Jane Doe"))
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    void updateProfileReturns409WhenEmailAlreadyTakenByAnotherUser() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Jane Doe", "taken@example.com");
        when(userService.updateProfile(eq(1L), any(UpdateProfileRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("Ya existe un usuario registrado con este correo electrónico"));

        mockMvc.perform(put("/api/users/profile")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateProfileReturns400WhenEmailIsBlank() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Jane Doe", "");

        mockMvc.perform(put("/api/users/profile")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfileReturns403WithoutAuthToken() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Jane Doe", "jane@example.com");

        mockMvc.perform(put("/api/users/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void changePasswordReturns204WhenCurrentPasswordIsCorrect() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPassword", "newPassword123");

        mockMvc.perform(put("/api/users/password")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    void changePasswordReturns401WhenCurrentPasswordIsWrong() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("wrongPassword", "newPassword123");
        doThrow(new InvalidCredentialsException("La contraseña actual no es correcta"))
                .when(userService).changePassword(eq(1L), any(ChangePasswordRequest.class));

        mockMvc.perform(put("/api/users/password")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordReturns400WhenNewPasswordIsTooShort() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPassword", "abc");

        mockMvc.perform(put("/api/users/password")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePasswordReturns403WithoutAuthToken() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPassword", "newPassword123");

        mockMvc.perform(put("/api/users/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
