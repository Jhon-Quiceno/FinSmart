package com.smartfinance.backend.common.config;

import com.smartfinance.backend.common.security.JwtAuthenticationFilter;
import com.smartfinance.backend.common.security.RateLimitFilter;
import com.smartfinance.backend.common.security.TelegramWebhookFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final TelegramWebhookFilter telegramWebhookFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RateLimitFilter rateLimitFilter,
            TelegramWebhookFilter telegramWebhookFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.telegramWebhookFilter = telegramWebhookFilter;
    }

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    // Same values as JwtProperties.refreshCookieSameSite/Secure, read directly from the
    // underlying env vars here (instead of injecting the JwtProperties bean) so @WebMvcTest
    // slices that @Import this config don't need to provide a JwtProperties bean just to
    // build the CSRF cookie repo. Reading app.jwt.refresh-cookie-secure itself wouldn't work
    // as a fallback source: that property is defined as ${JWT_REFRESH_COOKIE_SECURE} with no
    // default, so Spring resolves it to that literal unresolved placeholder (not "missing"),
    // and a @Value default only kicks in when the property is truly absent.
    @Value("${JWT_REFRESH_COOKIE_SAME_SITE:Lax}")
    private String csrfCookieSameSite;

    @Value("${JWT_REFRESH_COOKIE_SECURE:false}")
    private boolean csrfCookieSecure;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers(
                    "/api/users/register",
                    "/api/users/login",
                    "/api/users/logout",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/actuator/health",
                    // Server-to-server (n8n), sin sesión de usuario ni cookie CSRF: protegidas por
                    // TelegramWebhookFilter (secreto compartido) en lugar de CSRF.
                    "/api/integrations/telegram/confirm-link",
                    "/api/integrations/telegram/expenses",
                    "/api/integrations/telegram/receipts"
                )
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/users/register",
                    "/api/users/login",
                    "/api/users/csrf",
                    "/api/users/refresh",
                    "/api/users/logout",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/actuator/health",
                    // Server-to-server (n8n): la autenticación la hace TelegramWebhookFilter con
                    // un secreto compartido, no Spring Security/JWT.
                    "/api/integrations/telegram/confirm-link",
                    "/api/integrations/telegram/expenses",
                    "/api/integrations/telegram/receipts"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // Runs AFTER JwtAuthenticationFilter so /api/ai/chat can be rate-limited by IP+userId
            // instead of IP alone (see RateLimitFilter's class Javadoc for the full decision).
            .addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class)
            .addFilterBefore(telegramWebhookFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CSRF token stored in a cookie readable by the frontend (JavaScript).
     * The frontend must read XSRF-TOKEN cookie and send it as X-XSRF-TOKEN header
     * on requests that use cookie-based auth (e.g., POST /api/users/refresh).
     *
     * Same-site/secure policy mirrors the refresh cookie's. In production this
     * relies on the frontend proxying /api/* through its own origin (the rewrite
     * in smart-finance-frontend/next.config.mjs), which makes both cookies
     * first-party on the frontend domain, so SameSite=Lax works. Without that
     * proxy (frontend calling the Cloud Run origin directly), cookie-based CSRF
     * cannot work at all: JavaScript on the frontend domain can never read a
     * cookie set by the backend domain, regardless of the SameSite value.
     */
    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        repository.setCookieCustomizer(customizer -> customizer
            .sameSite(csrfCookieSameSite)
            .secure(csrfCookieSecure));
        return repository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
