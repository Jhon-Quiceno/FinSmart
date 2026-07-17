package com.smartfinance.backend.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinance.backend.common.config.JwtProperties;
import com.smartfinance.backend.common.config.SecurityConfig;
import com.smartfinance.backend.usuario.controller.UserController;
import com.smartfinance.backend.usuario.exception.InvalidCredentialsException;
import com.smartfinance.backend.usuario.model.dto.LoginRequest;
import com.smartfinance.backend.usuario.repository.UserRepository;
import com.smartfinance.backend.usuario.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Light integration test for {@link RateLimitFilter} wired through {@link SecurityConfig} into a
 * real controller — verifies the filter actually rejects requests once the configured limit is
 * exceeded, on the real {@code POST /api/users/login} route.
 *
 * <p>Overrides {@code app.rate-limit.login.*} to a tiny limit (2 requests / 60s) via
 * {@link TestPropertySource} so the test stays fast and doesn't depend on the production default.
 * {@code userService.login} is stubbed to always throw {@link InvalidCredentialsException} (401)
 * so allowed requests have a deterministic, cheap response to assert against — the point of this
 * test is the filter's behavior, not {@code UserController}'s.
 */
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.rate-limit.login.max-requests=2",
        "app.rate-limit.login.window-seconds=60"
})
class RateLimitFilterTest {

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

    @Test
    void loginShouldReturn429OnceRequestsExceedTheConfiguredLimit() throws Exception {
        when(userService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException("Credenciales inválidas"));
        LoginRequest request = new LoginRequest("jane@example.com", "wrong-password", false);
        String body = objectMapper.writeValueAsString(request);

        // First two requests are allowed through to the controller (which itself returns 401
        // for bad credentials) - the limit (2) has not been exceeded yet.
        mockMvc.perform(post("/api/users/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/users/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());

        // Third request within the same window is rejected by RateLimitFilter itself, before
        // reaching the controller.
        mockMvc.perform(post("/api/users/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value("Demasiadas solicitudes, intentá de nuevo en un momento."))
                .andExpect(jsonPath("$.path").value("/api/users/login"));
    }
}
