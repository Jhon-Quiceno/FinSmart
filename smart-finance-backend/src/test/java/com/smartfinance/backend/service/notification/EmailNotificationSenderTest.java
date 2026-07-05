package com.smartfinance.backend.service.notification;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class EmailNotificationSenderTest {

    @Test
    void sendShouldSkipSilentlyWhenMailSenderBeanIsAbsent() {
        EmailNotificationSender sender = new EmailNotificationSender(null, "", "");

        sender.send(buildRecipient(), "Título", "Mensaje");
        // No exception thrown, and there is no mailSender to verify against — silent no-op is the contract.
    }

    @Test
    void sendShouldSkipSilentlyWhenCredentialsAreBlank() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        EmailNotificationSender sender = new EmailNotificationSender(mailSender, "", "noreply@finsmart.app");

        sender.send(buildRecipient(), "Título", "Mensaje");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendShouldDeliverEmailWhenFullyConfigured() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        EmailNotificationSender sender = new EmailNotificationSender(mailSender, "brevo-user", "noreply@finsmart.app");

        sender.send(buildRecipient(), "Título", "Mensaje");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendShouldLogAndSwallowAnyExceptionRaisedDuringDelivery() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        Mockito.doThrow(new RuntimeException("unexpected"))
                .when(mailSender).send(any(SimpleMailMessage.class));
        EmailNotificationSender sender = new EmailNotificationSender(mailSender, "brevo-user", "noreply@finsmart.app");

        sender.send(buildRecipient(), "Título", "Mensaje");
        // No exception propagates — a failure here must never break notification delivery.
    }

    private EmailRecipient buildRecipient() {
        return new EmailRecipient(1L, "jhon@example.com", "Jhon");
    }
}
