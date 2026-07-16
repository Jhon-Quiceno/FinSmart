package com.smartfinance.backend.servicios.service.notification;

import com.smartfinance.backend.common.config.AsyncConfig;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * {@link NotificationSender} adapter that delivers notifications by email through Resend SMTP.
 *
 * <p>Degrades silently when the API key or a sender address are not configured (no
 * {@code RESEND_API_KEY}/{@code MAIL_FROM} set) — {@code spring.mail.host}
 * is always set to Resend's relay in {@code application.properties}, so
 * {@link JavaMailSender} is normally present as a bean regardless, but {@link #send} still
 * checks the actual credentials/from-address before attempting delivery, since a host with no
 * password will fail authentication rather than fail to boot. This is what lets the
 * application run with in-app-only notifications when no email provider is configured (see
 * {@code docs/sprints/sprint5.md}, architecture decision 6).
 *
 * <p>Sends a multipart/alternative message (plain text + branded HTML) rather than a
 * {@code SimpleMailMessage}: the plain-text part is the same string shown in-app (see
 * {@link NotificationDispatcher#dispatch}), so clients that can't render HTML — or spam
 * filters that penalize HTML-only mail — still get a readable notification.
 *
 * <p>{@code recipient.name()} is user-supplied (set at registration), so it is HTML-escaped
 * before being interpolated into {@link #HTML_TEMPLATE}, same as the subject/body — even
 * though both are currently server-generated strings, escaping them too costs nothing and
 * removes the need to re-audit this method if a future caller ever passes user input through.
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

    private static final String HTML_TEMPLATE = """
            <!DOCTYPE html>
            <html lang="es">
              <body style="margin:0;padding:24px 16px;background-color:#ecfdf5;background-image:linear-gradient(160deg,#ecfdf5 0%%,#f8fafc 55%%,#eff6ff 100%%);font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                  <tr>
                    <td align="center">
                      <table role="presentation" width="100%%" style="max-width:480px;background-color:#ffffff;border-radius:12px;overflow:hidden;" cellpadding="0" cellspacing="0">
                        <tr>
                          <td style="background-color:#15803d;padding:20px 24px;">
                            <span style="color:#ffffff;font-size:20px;font-weight:700;letter-spacing:0.3px;">KoroFin</span>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:28px 24px 8px 24px;">
                            <p style="margin:0 0 16px 0;color:#111827;font-size:15px;">Hola %s,</p>
                            <h1 style="margin:0 0 12px 0;color:#111827;font-size:18px;font-weight:600;">%s</h1>
                            <p style="margin:0;color:#374151;font-size:15px;line-height:1.5;">%s</p>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:24px;">
                            <hr style="border:none;border-top:1px solid #e5e7eb;margin:0 0 16px 0;">
                            <p style="margin:0;color:#9ca3af;font-size:12px;line-height:1.4;">
                              Este es un mensaje automático de KoroFin. Podés administrar tus preferencias de notificación desde la app.
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
              </body>
            </html>
            """;

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final boolean mailConfigured;

    public EmailNotificationSender(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${spring.mail.password:}") String mailPassword,
            @Value("${app.mail.from:}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.mailConfigured = mailSender != null && !mailPassword.isBlank() && !fromAddress.isBlank();

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
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(recipient.email());
            helper.setSubject(subject);
            helper.setText(body, buildHtmlBody(recipient.name(), subject, body));
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("failed_to_send_email_notification userId={}", recipient.userId(), ex);
        }
    }

    private static String buildHtmlBody(String recipientName, String subject, String body) {
        String escapedBody = HtmlUtils.htmlEscape(body).replace("\n", "<br>");
        return HTML_TEMPLATE.formatted(HtmlUtils.htmlEscape(recipientName), HtmlUtils.htmlEscape(subject), escapedBody);
    }
}
