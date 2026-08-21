package com.smartfinance.backend.servicios.service.notification;

import com.smartfinance.backend.servicios.model.entity.NotificationPreference;
import com.smartfinance.backend.servicios.model.entity.NotificationType;
import com.smartfinance.backend.servicios.model.entity.PushToken;
import com.smartfinance.backend.usuario.model.entity.User;
import com.smartfinance.backend.servicios.repository.NotificationPreferenceRepository;
import com.smartfinance.backend.servicios.repository.PushTokenRepository;
import com.smartfinance.backend.usuario.repository.UserRepository;
import com.smartfinance.backend.servicios.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationSender notificationSender;

    @Mock
    private PushTokenRepository pushTokenRepository;

    @Mock
    private PushNotificationSender pushNotificationSender;

    @InjectMocks
    private NotificationDispatcher notificationDispatcher;

    @Test
    void dispatchShouldCreateInAppOnlyWhenEmailDisabledInPreferences() {
        NotificationPreference preference = new NotificationPreference();
        preference.setPaymentReminders(true);
        preference.setEmailEnabled(false);
        when(notificationPreferenceRepository.findByUser_Id(1L)).thenReturn(Optional.of(preference));

        notificationDispatcher.dispatch(1L, NotificationType.PAYMENT_REMINDER, "Título", "Mensaje", "dedupe-1");

        verify(notificationService).createNotification(1L, NotificationType.PAYMENT_REMINDER, "Título", "Mensaje", "dedupe-1");
        verify(notificationSender, never()).send(any(EmailRecipient.class), anyString(), anyString());
    }

    @Test
    void dispatchShouldAlsoSendEmailWhenEmailEnabledInPreferences() {
        NotificationPreference preference = new NotificationPreference();
        preference.setOverspendAlerts(true);
        preference.setEmailEnabled(true);
        User user = new User();
        user.setId(1L);
        user.setEmail("jhon@example.com");
        user.setName("Jhon");
        when(notificationPreferenceRepository.findByUser_Id(1L)).thenReturn(Optional.of(preference));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        notificationDispatcher.dispatch(1L, NotificationType.OVERSPEND_ALERT, "Título", "Mensaje", null);

        verify(notificationService).createNotification(1L, NotificationType.OVERSPEND_ALERT, "Título", "Mensaje", null);
        verify(notificationSender).send(
                eq(new EmailRecipient(1L, "jhon@example.com", "Jhon")), eq("Título"), eq("Mensaje")
        );
    }

    @Test
    void dispatchShouldSkipEmailWhenUserNoLongerExists() {
        NotificationPreference preference = new NotificationPreference();
        preference.setOverspendAlerts(true);
        preference.setEmailEnabled(true);
        when(notificationPreferenceRepository.findByUser_Id(5L)).thenReturn(Optional.of(preference));
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        notificationDispatcher.dispatch(5L, NotificationType.OVERSPEND_ALERT, "Título", "Mensaje", null);

        verify(notificationService).createNotification(5L, NotificationType.OVERSPEND_ALERT, "Título", "Mensaje", null);
        verify(notificationSender, never()).send(any(EmailRecipient.class), anyString(), anyString());
    }

    @Test
    void dispatchShouldSkipEntirelyWhenTypeIsDisabledInPreferences() {
        NotificationPreference preference = new NotificationPreference();
        preference.setWeeklySummary(false);
        preference.setEmailEnabled(true);
        when(notificationPreferenceRepository.findByUser_Id(2L)).thenReturn(Optional.of(preference));

        notificationDispatcher.dispatch(2L, NotificationType.WEEKLY_SUMMARY, "Título", "Mensaje", null);

        verify(notificationService, never()).createNotification(any(), any(), anyString(), anyString(), any());
        verify(notificationSender, never()).send(any(EmailRecipient.class), anyString(), anyString());
    }

    @Test
    void dispatchShouldTreatMissingPreferencesRowAsAllTypesEnabledAndEmailDisabled() {
        when(notificationPreferenceRepository.findByUser_Id(3L)).thenReturn(Optional.empty());

        notificationDispatcher.dispatch(3L, NotificationType.INACTIVITY_REMINDER, "Título", "Mensaje", null);

        verify(notificationService).createNotification(3L, NotificationType.INACTIVITY_REMINDER, "Título", "Mensaje", null);
        verify(notificationSender, never()).send(any(EmailRecipient.class), anyString(), anyString());
    }

    @Test
    void dispatchShouldSkipEntirelyWhenCardCycleCloseIsDisabledInPreferences() {
        NotificationPreference preference = new NotificationPreference();
        preference.setCardCycleClose(false);
        preference.setEmailEnabled(true);
        when(notificationPreferenceRepository.findByUser_Id(6L)).thenReturn(Optional.of(preference));

        notificationDispatcher.dispatch(6L, NotificationType.CARD_CYCLE_CLOSE, "Título", "Mensaje", null);

        verify(notificationService, never()).createNotification(any(), any(), anyString(), anyString(), any());
        verify(notificationSender, never()).send(any(EmailRecipient.class), anyString(), anyString());
    }

    @Test
    void dispatchShouldCreateInAppWhenCardCycleCloseIsEnabledInPreferences() {
        NotificationPreference preference = new NotificationPreference();
        preference.setCardCycleClose(true);
        preference.setEmailEnabled(false);
        when(notificationPreferenceRepository.findByUser_Id(7L)).thenReturn(Optional.of(preference));

        notificationDispatcher.dispatch(7L, NotificationType.CARD_CYCLE_CLOSE, "Título", "Mensaje", "card-cycle-close:1:2026-07-15");

        verify(notificationService).createNotification(
                7L, NotificationType.CARD_CYCLE_CLOSE, "Título", "Mensaje", "card-cycle-close:1:2026-07-15"
        );
    }

    @Test
    void dispatchShouldAlwaysDeliverTypesWithoutADedicatedPreferenceToggle() {
        NotificationPreference preference = new NotificationPreference();
        preference.setEmailEnabled(false);
        when(notificationPreferenceRepository.findByUser_Id(4L)).thenReturn(Optional.of(preference));

        notificationDispatcher.dispatch(4L, NotificationType.MONTH_END_PREDICTION, "Título", "Mensaje", null);

        verify(notificationService).createNotification(4L, NotificationType.MONTH_END_PREDICTION, "Título", "Mensaje", null);
    }

    @Test
    void dispatchShouldSendPushToEveryTokenRegisteredByTheUser() {
        when(notificationPreferenceRepository.findByUser_Id(8L)).thenReturn(Optional.empty());
        PushToken firstDevice = new PushToken();
        firstDevice.setExpoPushToken("ExponentPushToken[first]");
        PushToken secondDevice = new PushToken();
        secondDevice.setExpoPushToken("ExponentPushToken[second]");
        when(pushTokenRepository.findByUser_Id(8L)).thenReturn(List.of(firstDevice, secondDevice));

        notificationDispatcher.dispatch(8L, NotificationType.PAYMENT_REMINDER, "Título", "Mensaje", null);

        verify(pushNotificationSender).send(eq(new PushRecipient(8L, "ExponentPushToken[first]")), eq("Título"), eq("Mensaje"));
        verify(pushNotificationSender).send(eq(new PushRecipient(8L, "ExponentPushToken[second]")), eq("Título"), eq("Mensaje"));
    }

    @Test
    void dispatchShouldSkipPushWhenUserHasNoRegisteredTokens() {
        when(notificationPreferenceRepository.findByUser_Id(9L)).thenReturn(Optional.empty());
        when(pushTokenRepository.findByUser_Id(9L)).thenReturn(List.of());

        notificationDispatcher.dispatch(9L, NotificationType.PAYMENT_REMINDER, "Título", "Mensaje", null);

        verify(notificationService).createNotification(9L, NotificationType.PAYMENT_REMINDER, "Título", "Mensaje", null);
        verify(pushNotificationSender, never()).send(any(PushRecipient.class), anyString(), anyString());
    }
}
