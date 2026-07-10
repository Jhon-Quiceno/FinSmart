package com.smartfinance.backend.servicios.service.notification;

import com.smartfinance.backend.common.config.AsyncConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * {@link NotificationSender} adapter that delivers notifications by email through Brevo SMTP.
 *
 * <p>Degrades silently when SMTP credentials or a sender address are not configured (no
 * {@code BREVO_SMTP_LOGIN}/{@code BREVO_SMTP_KEY}/{@code MAIL_FROM} set) — {@code spring.mail.host}
 * is always set to Brevo's relay in {@code application.properties}, so
 * {@link JavaMailSender} is normally present as a bean regardless, but {@link #send} still
 * checks the actual credentials/from-address before attempting delivery, since a host with no
 * username/password will fail authentication rather than fail to boot. This is what lets the
 * application run with in-app-only notifications when no email provider is configured (see
 * {@code docs/sprints/sprint5.md}, architecture decision 6).
 *
 * <p>{@link #send} catches {@code Exception} broadly rather than only {@link
 * org.springframework.mail.MailException}: this method runs on the mail {@code @Async} executor,
 * where any uncaught exception is only ever logged by Spring's default async exception handler
 * and never surfaces to a caller — a missing/failed email channel must never break notification
 * delivery, so every failure path here is caught and logged instead of left to propagate.
 */
@Component
public class EmailNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final boolean mailConfigured;

    public EmailNotificationSender(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String mailUsername,
            @Value("${app.mail.from:}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.mailConfigured = mailSender != null && !mailUsername.isBlank() && !fromAddress.isBlank();

        if (!mailConfigured) {
            log.info("email_notifications_disabled reason=missing_smtp_credentials_or_sender");
        }
    }

    @Async(AsyncConfig.MAIL_EXECUTOR)
    @Override
    public void send(EmailRecipient recipient, String subject, String body) {
        if (!mailConfigured) {
            log.debug("skip_email_notification userId={} reason=email_disabled", recipient.userId());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(recipient.email());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("failed_to_send_email_notification userId={}", recipient.userId(), ex);
        }
    }
}
