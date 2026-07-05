package com.smartfinance.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinance.backend.config.JwtProperties;
import com.smartfinance.backend.config.SecurityConfig;
import com.smartfinance.backend.dto.auth.ChangePasswordRequest;
import com.smartfinance.backend.dto.auth.UpdateProfileRequest;
import com.smartfinance.backend.dto.auth.UserResponse;
import com.smartfinance.backend.exception.EmailAlreadyExistsException;
import com.smartfinance.backend.exception.InvalidCredentialsException;
import com.smartfinance.backend.repository.UserRepository;
import com.smartfinance.backend.service.JwtService;
import com.smartfinance.backend.service.UserService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
