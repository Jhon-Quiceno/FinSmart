package com.smartfinance.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code PUT /api/users/password}. {@code newPassword} mirrors
 * {@link RegisterRequest}'s password length rule; {@code currentPassword} is verified against
 * the stored hash by {@code UserService#changePassword} before the change is applied.
 *
 * @param currentPassword the user's current password, in plain text
 * @param newPassword     the new password to set, in plain text
 */
public record ChangePasswordRequest(
        @NotBlank(message = "La contraseña actual es obligatoria")
        String currentPassword,
        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 6, max = 100, message = "La nueva contraseña debe tener entre 6 y 100 caracteres")
        String newPassword
) {
}
