package com.smartfinance.backend.controller;

import com.smartfinance.backend.config.JwtProperties;
import com.smartfinance.backend.dto.auth.AuthResponse;
import com.smartfinance.backend.dto.auth.ChangePasswordRequest;
import com.smartfinance.backend.dto.auth.LoginRequest;
import com.smartfinance.backend.dto.auth.RegisterRequest;
import com.smartfinance.backend.dto.auth.UpdateProfileRequest;
import com.smartfinance.backend.dto.auth.UserResponse;
import com.smartfinance.backend.dto.error.ErrorResponse;
import com.smartfinance.backend.security.SecurityUtils;
import com.smartfinance.backend.service.AuthSession;
import com.smartfinance.backend.service.UserService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthSession session = userService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(session.refreshToken()).toString())
                .body(session.response());
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
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthSession session = userService.login(request);
        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(session.refreshToken()).toString())
                .body(session.response());
    }

    @Operation(summary = "Refrescar sesion", description = "Renueva access token y refresh token desde cookie HttpOnly")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesion refrescada exitosamente"),
            @ApiResponse(responseCode = "401", description = "Refresh token invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        String refreshToken = resolveRefreshToken(request);
        AuthSession session = userService.refresh(refreshToken);
        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(session.refreshToken()).toString())
                .body(session.response());
    }

    @Operation(summary = "Obtener CSRF token", description = "Genera/retorna token CSRF y lo expone para el frontend SPA")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "CSRF token disponible")
    })
    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok(Map.of("token", csrfToken.getToken()));
    }

    @Operation(summary = "Cerrar sesion", description = "Revoca refresh token y limpia cookie")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sesion cerrada exitosamente")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String refreshToken = resolveRefreshToken(request);
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

    private String resolveRefreshToken(HttpServletRequest request) {
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

    private ResponseCookie buildRefreshCookie(String refreshToken) {
        return ResponseCookie.from(jwtProperties.refreshCookieName(), refreshToken)
                .httpOnly(true)
                .secure(jwtProperties.refreshCookieSecure())
                .sameSite(jwtProperties.refreshCookieSameSite())
                .path("/")
                .maxAge(Duration.ofMillis(jwtProperties.refreshExpirationMs()))
                .build();
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
