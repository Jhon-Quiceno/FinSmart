package com.smartfinance.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code PUT /api/users/profile}, mirroring {@link RegisterRequest}'s name/email
 * validation rules.
 *
 * @param name  new display name
 * @param email new email address; must not collide with another user's email
 *              (see {@code UserService#updateProfile})
 */
public record UpdateProfileRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String name,
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico no es válido")
        @Size(max = 180, message = "El correo electrónico no puede superar 180 caracteres")
        String email
) {
}
