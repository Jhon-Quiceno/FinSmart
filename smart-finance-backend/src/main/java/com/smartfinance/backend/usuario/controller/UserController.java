package com.smartfinance.backend.usuario.controller;

import com.smartfinance.backend.common.config.JwtProperties;
import com.smartfinance.backend.usuario.model.dto.AuthResponse;
import com.smartfinance.backend.usuario.model.dto.ChangePasswordRequest;
import com.smartfinance.backend.usuario.model.dto.LoginRequest;
import com.smartfinance.backend.usuario.model.dto.RefreshRequest;
import com.smartfinance.backend.usuario.model.dto.RegisterRequest;
import com.smartfinance.backend.usuario.model.dto.UpdateProfileRequest;
import com.smartfinance.backend.usuario.model.dto.UpdateUserPreferencesRequest;
import com.smartfinance.backend.usuario.model.dto.UserPreferencesResponse;
import com.smartfinance.backend.usuario.model.dto.UserResponse;
import com.smartfinance.backend.common.exception.ErrorResponse;
import com.smartfinance.backend.common.security.SecurityUtils;
import com.smartfinance.backend.usuario.service.AuthSession;
import com.smartfinance.backend.usuario.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Autenticacion", description = "Endpoints para registro, login y sesion de usuarios")
public class UserController {

    private final UserService userService;
    private final JwtProperties jwtProperties;

    public UserController(UserService userService, JwtProperties jwtProperties) {
        this.userService = userService;
        this.jwtProperties = jwtProperties;
    }

    @Operation(summary = "Registrar nuevo usuario", description = "Crea una cuenta nueva y devuelve access token")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe un usuario con ese correo",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader(value = "X-Client", required = false) String client
    ) {
        AuthSession session = userService.register(request);
        return buildAuthResponseEntity(session, HttpStatus.CREATED, isMobileClient(client));
    }

    @Operation(summary = "Iniciar sesion", description = "Autentica un usuario y devuelve access token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesion iniciada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciales invalidas",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Client", required = false) String client
    ) {
        AuthSession session = userService.login(request);
        return buildAuthResponseEntity(session, HttpStatus.OK, isMobileClient(client));
    }

    @Operation(summary = "Refrescar sesion", description = "Renueva access token y refresh token desde cookie HttpOnly o, para clientes mobile sin cookie, desde el cuerpo de la peticion")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesion refrescada exitosamente"),
            @ApiResponse(responseCode = "401", description = "Refresh token invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            HttpServletRequest request,
            @RequestBody(required = false) RefreshRequest body,
            @RequestHeader(value = "X-Client", required = false) String client
    ) {
        String refreshToken = resolveRefreshToken(request, body);
        AuthSession session = userService.refresh(refreshToken);
        return buildAuthResponseEntity(session, HttpStatus.OK, isMobileClient(client));
    }

    @Operation(summary = "Obtener CSRF token", description = "Genera/retorna token CSRF y lo expone para el frontend SPA")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "CSRF token disponible")
    })
    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok(Map.of("token", csrfToken.getToken()));
    }

    @Operation(summary = "Cerrar sesion", description = "Revoca refresh token (desde cookie HttpOnly o, para clientes mobile sin cookie, desde el cuerpo de la peticion) y limpia cookie")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sesion cerrada exitosamente")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, @RequestBody(required = false) RefreshRequest body) {
        String refreshToken = resolveRefreshToken(request, body);
        userService.logout(refreshToken);

        return ResponseEntity
                .noContent()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .build();
    }

    @Operation(summary = "Actualizar perfil", description = "Actualiza el nombre y correo del usuario autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ya existe un usuario con ese correo",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    @Operation(summary = "Cambiar contraseña", description = "Cambia la contraseña del usuario autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Contraseña cambiada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "La contraseña actual no es correcta",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        userService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Obtener preferencias", description = "Devuelve el tema, la moneda y el idioma del usuario autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preferencias disponibles")
    })
    @GetMapping("/preferences")
    public ResponseEntity<UserPreferencesResponse> getPreferences() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.getPreferences(userId));
    }

    @Operation(summary = "Actualizar preferencias", description = "Actualiza el tema, la moneda y el idioma del usuario autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preferencias actualizadas exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/preferences")
    public ResponseEntity<UserPreferencesResponse> updatePreferences(@Valid @RequestBody UpdateUserPreferencesRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.updatePreferences(userId, request));
    }

    /**
     * Mobile clients (Bearer-token-only, no ambient cookies) identify themselves with
     * {@code X-Client: mobile}. For them, the refresh token travels in the response body instead
     * of an {@code HttpOnly} cookie — see {@link AuthResponse}'s Javadoc — and every mutating
     * request carrying that same header is exempt from CSRF (see {@code SecurityConfig}).
     * {@code /refresh} and {@code /logout} also accept the token back via {@link RefreshRequest}
     * in the request body for these clients, since they have no cookie to send it in — see
     * {@link #resolveRefreshToken(HttpServletRequest, RefreshRequest)}. Browser clients (the
     * default, no header) keep the cookie-only behavior unchanged.
     */
    private boolean isMobileClient(String client) {
        return "mobile".equals(client);
    }

    private ResponseEntity<AuthResponse> buildAuthResponseEntity(AuthSession session, HttpStatus status, boolean mobile) {
        if (mobile) {
            AuthResponse bodyWithRefreshToken = new AuthResponse(
                    session.response().accessToken(),
                    session.response().tokenType(),
                    session.response().expiresIn(),
                    session.response().user(),
                    session.refreshToken()
            );
            return ResponseEntity.status(status).body(bodyWithRefreshToken);
        }

        return ResponseEntity
                .status(status)
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(session.refreshToken(), session.rememberMe()).toString())
                .body(session.response());
    }

    private String resolveRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (jwtProperties.refreshCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    /**
     * Cookie first, body second. A browser never sends a body, so its path is unchanged; a mobile
     * client never has a cookie, so it falls through to the body. If somehow both are present, the
     * cookie wins — it's the source the server controls ({@code HttpOnly}), not one page JavaScript
     * could have written.
     */
    private String resolveRefreshToken(HttpServletRequest request, RefreshRequest body) {
        String fromCookie = resolveRefreshTokenFromCookie(request);
        if (fromCookie != null && !fromCookie.isBlank()) {
            return fromCookie;
        }
        return body == null ? null : body.refreshToken();
    }

    /**
     * When {@code rememberMe} is {@code true}, the cookie carries {@code Max-Age} so it survives
     * browser restarts; when {@code false}, {@code Max-Age} is omitted entirely so the browser
     * treats it as a session cookie and drops it when the browser closes. Either way, the JWT
     * refresh token embedded in the cookie keeps its normal expiration.
     */
    private ResponseCookie buildRefreshCookie(String refreshToken, boolean rememberMe) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(jwtProperties.refreshCookieName(), refreshToken)
                .httpOnly(true)
                .secure(jwtProperties.refreshCookieSecure())
                .sameSite(jwtProperties.refreshCookieSameSite())
                .path("/");

        if (rememberMe) {
            builder.maxAge(Duration.ofMillis(jwtProperties.refreshExpirationMs()));
        }

        return builder.build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(jwtProperties.refreshCookieName(), "")
                .httpOnly(true)
                .secure(jwtProperties.refreshCookieSecure())
                .sameSite(jwtProperties.refreshCookieSameSite())
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }
}
