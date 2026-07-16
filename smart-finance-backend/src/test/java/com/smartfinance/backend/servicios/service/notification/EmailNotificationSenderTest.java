package com.smartfinance.backend.servicios.service.notification;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

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
        JavaMailSender mailSender = mockMailSender();
        EmailNotificationSender sender = new EmailNotificationSender(mailSender, "", "noreply@korofin.app");

        sender.send(buildRecipient(), "Título", "Mensaje");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendShouldDeliverEmailWhenFullyConfigured() {
        JavaMailSender mailSender = mockMailSender();
        EmailNotificationSender sender = new EmailNotificationSender(mailSender, "resend-api-key", "noreply@korofin.app");

        sender.send(buildRecipient(), "Título", "Mensaje");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendShouldLogAndSwallowAnyExceptionRaisedDuringDelivery() {
        JavaMailSender mailSender = mockMailSender();
        Mockito.doThrow(new RuntimeException("unexpected"))
                .when(mailSender).send(any(MimeMessage.class));
        EmailNotificationSender sender = new EmailNotificationSender(mailSender, "resend-api-key", "noreply@korofin.app");

        sender.send(buildRecipient(), "Título", "Mensaje");
        // No exception propagates — a failure here must never break notification delivery.
    }

    private static JavaMailSender mockMailSender() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        // createMimeMessage() needs a real MimeMessage backed by a Session: EmailNotificationSender
        // passes it straight into MimeMessageHelper, which requires a non-null, usable instance.
        Mockito.when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        return mailSender;
    }

    private EmailRecipient buildRecipient() {
        return new EmailRecipient(1L, "jhon@example.com", "Jhon");
    }
}
