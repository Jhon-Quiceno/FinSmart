package com.smartfinance.backend.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Set;

/**
 * Autentica las tres rutas server-to-server llamadas por n8n en nombre del bot de Telegram
 * ({@code /api/integrations/telegram/confirm-link}, {@code /api/integrations/telegram/expenses} y
 * {@code /api/integrations/telegram/receipts}) mediante un secreto compartido en el header
 * {@code X-Telegram-Webhook-Secret}, en lugar de JWT (n8n no tiene una sesión de usuario).
 *
 * <p>Modelado sobre {@link RateLimitFilter}: {@code @Component} con dependencias limitadas a
 * primitivos inyectados por {@code @Value}, para seguir siendo trivialmente construible en los
 * slices {@code @WebMvcTest} que no configuran el contexto completo.
 *
 * <p><b>Secreto vacío = integración deshabilitada.</b> Si {@code app.integrations.telegram.webhook-secret}
 * no está configurado, todo request a estas dos rutas se rechaza con 401, incluso si el header
 * llega vacío o ausente — nunca se acepta un secreto vacío como válido.
 *
 * <p>La comparación usa {@link MessageDigest#isEqual(byte[], byte[])} (tiempo constante) para no
 * filtrar, a través de temporización, cuán cerca estuvo un secreto incorrecto del real.
 */
@Component
public class TelegramWebhookFilter extends OncePerRequestFilter {

    private static final String CONFIRM_LINK_PATH = "/api/integrations/telegram/confirm-link";
    private static final String EXPENSES_PATH = "/api/integrations/telegram/expenses";
    private static final String RECEIPTS_PATH = "/api/integrations/telegram/receipts";
    private static final Set<String> PROTECTED_PATHS = Set.of(CONFIRM_LINK_PATH, EXPENSES_PATH, RECEIPTS_PATH);
    private static final String SECRET_HEADER = "X-Telegram-Webhook-Secret";

    private final String webhookSecret;

    public TelegramWebhookFilter(
            @Value("${app.integrations.telegram.webhook-secret:}") String webhookSecret
    ) {
        this.webhookSecret = webhookSecret;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !PROTECTED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (isAuthorized(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        writeUnauthorized(response, request.getRequestURI());
    }

    private boolean isAuthorized(HttpServletRequest request) {
        if (webhookSecret.isBlank()) {
            return false;
        }

        String providedSecret = request.getHeader(SECRET_HEADER);
        if (providedSecret == null) {
            return false;
        }

        return MessageDigest.isEqual(
                webhookSecret.getBytes(StandardCharsets.UTF_8),
                providedSecret.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static void writeUnauthorized(HttpServletResponse response, String path) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        String body = """
                {"timestamp":"%s","status":401,"error":"Unauthorized","message":"No autorizado","path":"%s"}"""
                .formatted(Instant.now(), escapeJson(path));
        response.getWriter().write(body);
    }

    private static String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
