package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.notification.NotificationPreferenceRequest;
import com.smartfinance.backend.dto.notification.NotificationPreferenceResponse;
import com.smartfinance.backend.dto.notification.NotificationResponse;
import com.smartfinance.backend.exception.ResourceNotFoundException;
import com.smartfinance.backend.mapper.NotificationMapper;
import com.smartfinance.backend.mapper.NotificationPreferenceMapper;
import com.smartfinance.backend.model.Notification;
import com.smartfinance.backend.model.NotificationPreference;
import com.smartfinance.backend.model.NotificationType;
import com.smartfinance.backend.model.User;
import com.smartfinance.backend.repository.NotificationPreferenceRepository;
import com.smartfinance.backend.repository.NotificationRepository;
import com.smartfinance.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private NotificationPreferenceMapper notificationPreferenceMapper;

    @InjectMocks
    private NotificationService notificationService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createNotificationShouldSkipWhenDedupeKeyAlreadyExistsForUser() {
        when(notificationRepository.existsByUser_IdAndDedupeKey(1L, "PAYMENT_REMINDER:debt:5:2026-07"))
                .thenReturn(true);

        notificationService.createNotification(
                1L, NotificationType.PAYMENT_REMINDER, "Recordatorio", "Tu deuda vence pronto",
                "PAYMENT_REMINDER:debt:5:2026-07"
        );

        verify(notificationRepository, never()).saveAndFlush(any(Notification.class));
    }

    @Test
    void createNotificationShouldSkipWhenDatabaseRejectsRaceDuplicate() {
        when(notificationRepository.existsByUser_IdAndDedupeKey(1L, "WEEKLY_SUMMARY:2026-W27"))
                .thenReturn(false);
        when(userRepository.getReferenceById(1L)).thenReturn(buildUser(1L));
        when(notificationRepository.saveAndFlush(any(Notification.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        Assertions.assertDoesNotThrow(() -> notificationService.createNotification(
                1L, NotificationType.WEEKLY_SUMMARY, "Resumen semanal", "Tu resumen está listo",
                "WEEKLY_SUMMARY:2026-W27"
        ));
    }

    @Test
    void createNotificationShouldSaveWhenNoDedupeKeyProvided() {
        when(userRepository.getReferenceById(2L)).thenReturn(buildUser(2L));
        when(notificationRepository.saveAndFlush(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.createNotification(2L, NotificationType.SYSTEM, "Aviso", "Mensaje del sistema", null);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).saveAndFlush(captor.capture());
        Assertions.assertEquals(NotificationType.SYSTEM, captor.getValue().getType());
        Assertions.assertFalse(captor.getValue().isRead());
        Assertions.assertNull(captor.getValue().getDedupeKey());
    }

    @Test
    void getPreferencesShouldCreateDefaultsWhenNoneExistYet() {
        setAuthenticatedUser(3L);
        when(notificationPreferenceRepository.findByUser_Id(3L)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(3L)).thenReturn(buildUser(3L));
        when(notificationPreferenceRepository.save(any(NotificationPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        NotificationPreferenceResponse expectedResponse = new NotificationPreferenceResponse(
                true, true, true, true, false
        );
        when(notificationPreferenceMapper.toResponse(any(NotificationPreference.class))).thenReturn(expectedResponse);

        NotificationPreferenceResponse response = notificationService.getPreferences();

        ArgumentCaptor<NotificationPreference> captor = ArgumentCaptor.forClass(NotificationPreference.class);
        verify(notificationPreferenceRepository).save(captor.capture());
        Assertions.assertTrue(captor.getValue().isPaymentReminders());
        Assertions.assertTrue(captor.getValue().isOverspendAlerts());
        Assertions.assertTrue(captor.getValue().isWeeklySummary());
        Assertions.assertTrue(captor.getValue().isInactivityReminders());
        Assertions.assertFalse(captor.getValue().isEmailEnabled());
        Assertions.assertEquals(expectedResponse, response);
    }

    @Test
    void getPreferencesShouldReturnExistingRowWithoutCreatingANewOne() {
        setAuthenticatedUser(4L);
        NotificationPreference existing = new NotificationPreference();
        existing.setId(10L);
        when(notificationPreferenceRepository.findByUser_Id(4L)).thenReturn(Optional.of(existing));
        when(notificationPreferenceMapper.toResponse(existing))
                .thenReturn(new NotificationPreferenceResponse(true, false, true, false, true));

        notificationService.getPreferences();

        verify(notificationPreferenceRepository, never()).save(any(NotificationPreference.class));
    }

    @Test
    void updatePreferencesShouldApplyRequestOntoExistingRow() {
        setAuthenticatedUser(5L);
        NotificationPreference existing = new NotificationPreference();
        existing.setId(11L);
        NotificationPreferenceRequest request = new NotificationPreferenceRequest(false, false, false, false, true);
        when(notificationPreferenceRepository.findByUser_Id(5L)).thenReturn(Optional.of(existing));
        when(notificationPreferenceRepository.save(existing)).thenReturn(existing);
        when(notificationPreferenceMapper.toResponse(existing))
                .thenReturn(new NotificationPreferenceResponse(false, false, false, false, true));

        NotificationPreferenceResponse response = notificationService.updatePreferences(request);

        verify(notificationPreferenceMapper).updateEntityFromRequest(request, existing);
        Assertions.assertFalse(response.paymentReminders());
        Assertions.assertTrue(response.emailEnabled());
    }

    @Test
    void markAsReadShouldSetReadAndReadAtWhenOwnedByCurrentUser() {
        setAuthenticatedUser(6L);
        Notification notification = new Notification();
        notification.setId(20L);
        notification.setUser(buildUser(6L));
        notification.setRead(false);
        when(notificationRepository.findByIdAndUser_Id(20L, 6L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);
        when(notificationMapper.toResponse(notification)).thenReturn(
                new NotificationResponse(20L, NotificationType.SYSTEM, "t", "m", true, Instant.now(), Instant.now())
        );

        notificationService.markAsRead(20L);

        Assertions.assertTrue(notification.isRead());
        Assertions.assertNotNull(notification.getReadAt());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsReadShouldThrowNotFoundWhenOwnedByAnotherUser() {
        setAuthenticatedUser(6L);
        when(notificationRepository.findByIdAndUser_Id(99L, 6L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> notificationService.markAsRead(99L));
    }

    @Test
    void markAllAsReadShouldDelegateToBulkUpdateForCurrentUser() {
        setAuthenticatedUser(7L);

        notificationService.markAllAsRead();

        verify(notificationRepository, times(1)).markAllAsReadForUser(7L);
    }

    @Test
    void getUnreadCountShouldReturnCountForCurrentUser() {
        setAuthenticatedUser(8L);
        when(notificationRepository.countByUser_IdAndReadFalse(8L)).thenReturn(3L);

        long count = notificationService.getUnreadCount();

        Assertions.assertEquals(3L, count);
    }

    @Test
    void getNotificationsShouldReturnMappedPageForCurrentUser() {
        setAuthenticatedUser(9L);
        Pageable pageable = PageRequest.of(0, 20);
        Notification notification = new Notification();
        notification.setId(30L);
        Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);
        when(notificationRepository.findByUser_IdOrderByCreatedAtDesc(eq(9L), any(Pageable.class))).thenReturn(page);
        when(notificationMapper.toResponse(notification)).thenReturn(
                new NotificationResponse(30L, NotificationType.SYSTEM, "t", "m", false, null, Instant.now())
        );

        Page<NotificationResponse> result = notificationService.getNotifications(pageable);

        Assertions.assertEquals(1, result.getTotalElements());
        Assertions.assertEquals(30L, result.getContent().get(0).id());
    }

    private void setAuthenticatedUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null)
        );
    }

    private User buildUser(Long userId) {
        User user = new User();
        user.setId(userId);
        return user;
    }
}
