package com.smartfinance.backend.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * Basic in-memory rate limiting for the endpoints most exposed to abuse: login, register, and the
 * two routes that fan out to a paid/rate-limited AI provider per call ({@code /api/ai/chat} and
 * {@code /api/receipts/scan}).
 *
 * <p><b>Filter order decision:</b> registered via {@code addFilterAfter(rateLimitFilter,
 * JwtAuthenticationFilter.class)} in {@code SecurityConfig} — running after
 * {@link JwtAuthenticationFilter} means {@link SecurityContextHolder} already has the
 * authenticated user (if any) by the time this filter runs, so the AI-backed routes can be keyed
 * by IP+userId instead of IP alone. The simpler alternative (IP-only for every route, filter
 * order irrelevant) was considered and rejected: two different users behind the same IP
 * (office/NAT) would otherwise share one bucket, which is a worse failure mode than the
 * small added complexity of reading the security context here.
 *
 * <p><b>Constructor dependencies are intentionally limited to {@code @Value}-injected
 * primitives</b> (no {@link InMemoryRateLimiter} bean, no {@code ObjectMapper} bean): this filter
 * is a {@code @Component} implementing {@link jakarta.servlet.Filter}, so Spring Boot's
 * {@code @WebMvcTest} slices auto-detect and instantiate it in every controller test in the
 * module (the same mechanism that already applies to {@link JwtAuthenticationFilter}), whether or
 * not that test explicitly {@code @Import}s {@code SecurityConfig}. Depending on any additional
 * Spring-managed bean here would require every existing {@code @WebMvcTest} to provide one, so
 * {@link InMemoryRateLimiter} is constructed directly (see its Javadoc) and the 429 body is built
 * as a plain string instead of via an autowired {@code ObjectMapper}.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/users/login";
    private static final String REGISTER_PATH = "/api/users/register";
    private static final String AI_CHAT_PATH = "/api/ai/chat";
    private static final String RECEIPT_SCAN_PATH = "/api/receipts/scan";

    private final InMemoryRateLimiter rateLimiter = new InMemoryRateLimiter();

    private final int loginMaxRequests;
    private final Duration loginWindow;
    private final int registerMaxRequests;
    private final Duration registerWindow;
    private final int aiChatMaxRequests;
    private final Duration aiChatWindow;
    private final int receiptScanMaxRequests;
    private final Duration receiptScanWindow;

    public RateLimitFilter(
            @Value("${app.rate-limit.login.max-requests:5}") int loginMaxRequests,
            @Value("${app.rate-limit.login.window-seconds:60}") long loginWindowSeconds,
            @Value("${app.rate-limit.register.max-requests:3}") int registerMaxRequests,
            @Value("${app.rate-limit.register.window-seconds:300}") long registerWindowSeconds,
            @Value("${app.rate-limit.ai-chat.max-requests:10}") int aiChatMaxRequests,
            @Value("${app.rate-limit.ai-chat.window-seconds:60}") long aiChatWindowSeconds,
            @Value("${app.rate-limit.receipt-scan.max-requests:10}") int receiptScanMaxRequests,
            @Value("${app.rate-limit.receipt-scan.window-seconds:60}") long receiptScanWindowSeconds
    ) {
        this.loginMaxRequests = loginMaxRequests;
        this.loginWindow = Duration.ofSeconds(loginWindowSeconds);
        this.registerMaxRequests = registerMaxRequests;
        this.registerWindow = Duration.ofSeconds(registerWindowSeconds);
        this.aiChatMaxRequests = aiChatMaxRequests;
        this.aiChatWindow = Duration.ofSeconds(aiChatWindowSeconds);
        this.receiptScanMaxRequests = receiptScanMaxRequests;
        this.receiptScanWindow = Duration.ofSeconds(receiptScanWindowSeconds);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        RateLimitRule rule = resolveRule(path, request);

        if (rule == null || rateLimiter.tryConsume(rule.key(), rule.maxRequests(), rule.window())) {
            filterChain.doFilter(request, response);
            return;
        }

        writeTooManyRequests(response, path);
    }

    private RateLimitRule resolveRule(String path, HttpServletRequest request) {
        if (LOGIN_PATH.equals(path)) {
            return new RateLimitRule(clientIp(request) + ":login", loginMaxRequests, loginWindow);
        }
        if (REGISTER_PATH.equals(path)) {
            return new RateLimitRule(clientIp(request) + ":register", registerMaxRequests, registerWindow);
        }
        if (AI_CHAT_PATH.equals(path)) {
            return new RateLimitRule(authenticatedKey(request, "ai-chat"), aiChatMaxRequests, aiChatWindow);
        }
        if (RECEIPT_SCAN_PATH.equals(path)) {
            return new RateLimitRule(authenticatedKey(request, "receipt-scan"), receiptScanMaxRequests, receiptScanWindow);
        }
        return null;
    }

    /**
     * Misma idea que la clave de {@code /api/ai/chat}: IP+userId en vez de solo IP, para que dos
     * usuarios autenticados detrás del mismo NAT/oficina no compartan un mismo balde. Usada por
     * ambas rutas que disparan una llamada de IA paga por request ({@code /api/ai/chat} y
     * {@code /api/receipts/scan}).
     */
    private String authenticatedKey(HttpServletRequest request, String bucket) {
        String ip = clientIp(request);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() != null) {
            return ip + ":" + bucket + ":" + authentication.getPrincipal();
        }
        return ip + ":" + bucket;
    }

    /**
     * Usa siempre la IP del socket ({@code request.getRemoteAddr()}), nunca el header
     * {@code X-Forwarded-For}: ese header lo controla el cliente que hace el request, y sin una
     * lista de proxies de confianza configurada (no existe hoy en este servicio) confiar en su
     * primer valor permite a un atacante rotar IPs falsas para esquivar el rate limit por
     * completo. Si en el futuro el backend queda detrás de un proxy/load balancer real, esta
     * lógica debe actualizarse junto con esa configuración (ej. {@code ForwardedHeaderFilter} con
     * proxies de confianza declarados), no antes.
     */
    private static String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private static void writeTooManyRequests(HttpServletResponse response, String path) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        String body = """
                {"timestamp":"%s","status":429,"error":"Too Many Requests","message":"Demasiadas solicitudes, intentá de nuevo en un momento.","path":"%s"}"""
                .formatted(Instant.now(), escapeJson(path));
        response.getWriter().write(body);
    }

    private static String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record RateLimitRule(String key, int maxRequests, Duration window) {
    }
}
