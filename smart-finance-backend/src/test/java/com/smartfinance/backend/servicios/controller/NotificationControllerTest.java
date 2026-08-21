package com.smartfinance.backend.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartfinance.backend.common.config.SecurityConfig;
import com.smartfinance.backend.servicios.model.dto.NotificationPreferenceRequest;
import com.smartfinance.backend.servicios.model.dto.NotificationPreferenceResponse;
import com.smartfinance.backend.servicios.model.dto.NotificationResponse;
import com.smartfinance.backend.servicios.model.dto.PushTokenRequest;
import com.smartfinance.backend.common.exception.ResourceNotFoundException;
import com.smartfinance.backend.servicios.model.entity.NotificationType;
import com.smartfinance.backend.usuario.repository.UserRepository;
import com.smartfinance.backend.common.security.JwtService;
import com.smartfinance.backend.servicios.service.NotificationService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private static final String AUTH_HEADER = "Bearer test-token";

    private final NotificationResponse paymentReminder = new NotificationResponse(
            1L, NotificationType.PAYMENT_REMINDER, "Recordatorio de pago", "Tu deuda vence en 3 días",
            false, null, Instant.parse("2026-07-01T10:00:00Z")
    );

    private final NotificationPreferenceResponse defaultPreferences = new NotificationPreferenceResponse(
            true, true, true, true, true, false
    );

    @BeforeEach
    void setUp() {
        Claims mockClaims = org.mockito.Mockito.mock(Claims.class);
        when(mockClaims.getSubject()).thenReturn("1");
        when(jwtService.parseAccessToken(any())).thenReturn(mockClaims);
        when(userRepository.existsById(1L)).thenReturn(true);
    }

    @Test
    void getNotificationsReturns200WithPagedResults() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        when(notificationService.getNotifications(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(paymentReminder), pageable, 1));

        mockMvc.perform(get("/api/notifications").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].type").value("PAYMENT_REMINDER"));
    }

    @Test
    void getNotificationsReturns401WithoutAuthToken() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUnreadCountReturns200WithCount() throws Exception {
        when(notificationService.getUnreadCount()).thenReturn(4L);

        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(4));
    }

    @Test
    void markAsReadReturns200WhenOwnedByCurrentUser() throws Exception {
        NotificationResponse readResponse = new NotificationResponse(
                1L, NotificationType.PAYMENT_REMINDER, "Recordatorio de pago", "Tu deuda vence en 3 días",
                true, Instant.parse("2026-07-01T11:00:00Z"), Instant.parse("2026-07-01T10:00:00Z")
        );
        when(notificationService.markAsRead(1L)).thenReturn(readResponse);

        mockMvc.perform(patch("/api/notifications/1/read").header("Authorization", AUTH_HEADER).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void markAsReadReturns404WhenOwnedByAnotherUser() throws Exception {
        when(notificationService.markAsRead(99L))
                .thenThrow(new ResourceNotFoundException("Notificación no encontrada"));

        mockMvc.perform(patch("/api/notifications/99/read").header("Authorization", AUTH_HEADER).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void markAllAsReadReturns204() throws Exception {
        mockMvc.perform(patch("/api/notifications/read-all").header("Authorization", AUTH_HEADER).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void getPreferencesReturns200WithCurrentPreferences() throws Exception {
        when(notificationService.getPreferences()).thenReturn(defaultPreferences);

        mockMvc.perform(get("/api/notifications/preferences").header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentReminders").value(true))
                .andExpect(jsonPath("$.emailEnabled").value(false));
    }

    @Test
    void updatePreferencesReturns200WhenValid() throws Exception {
        NotificationPreferenceRequest request = new NotificationPreferenceRequest(false, true, false, true, true, true);
        when(notificationService.updatePreferences(eq(request))).thenReturn(
                new NotificationPreferenceResponse(false, true, false, true, true, true)
        );

        mockMvc.perform(put("/api/notifications/preferences")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentReminders").value(false))
                .andExpect(jsonPath("$.emailEnabled").value(true));
    }

    @Test
    void updatePreferencesReturns400WhenFieldIsMissing() throws Exception {
        String invalidBody = """
                {"paymentReminders": true, "overspendAlerts": true, "weeklySummary": true, "inactivityReminders": true}
                """;

        mockMvc.perform(put("/api/notifications/preferences")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerPushTokenReturns204WhenValid() throws Exception {
        PushTokenRequest request = new PushTokenRequest("ExponentPushToken[abc123]", "device-1");

        mockMvc.perform(post("/api/notifications/push-token")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(notificationService).registerPushToken(request);
    }

    @Test
    void registerPushTokenReturns400WhenExpoPushTokenIsBlank() throws Exception {
        String invalidBody = """
                {"expoPushToken": "", "deviceId": "device-1"}
                """;

        mockMvc.perform(post("/api/notifications/push-token")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mutatingRequestWithMobileHeaderSkipsCsrf() throws Exception {
        // No .with(csrf()): the X-Client: mobile header alone must be enough to bypass CSRF,
        // on a path that is NOT one of the unconditionally-exempt ones (see SecurityConfig).
        PushTokenRequest request = new PushTokenRequest("ExponentPushToken[abc123]", "device-1");

        mockMvc.perform(post("/api/notifications/push-token")
                        .header("Authorization", AUTH_HEADER)
                        .header("X-Client", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    void mutatingRequestWithoutMobileHeaderStillRequiresCsrf() throws Exception {
        PushTokenRequest request = new PushTokenRequest("ExponentPushToken[abc123]", "device-1");

        mockMvc.perform(post("/api/notifications/push-token")
                        .header("Authorization", AUTH_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unregisterPushTokenReturns204() throws Exception {
        mockMvc.perform(delete("/api/notifications/push-token/device-1")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(notificationService).unregisterPushToken("device-1");
    }

    @Test
    void unregisterPushTokenWithMobileHeaderSkipsCsrf() throws Exception {
        // Same CSRF-exemption contract as mutatingRequestWithMobileHeaderSkipsCsrf, exercised on
        // the DELETE endpoint the real mobile app will call on logout.
        mockMvc.perform(delete("/api/notifications/push-token/device-1")
                        .header("Authorization", AUTH_HEADER)
                        .header("X-Client", "mobile"))
                .andExpect(status().isNoContent());

        verify(notificationService).unregisterPushToken("device-1");
    }
}
