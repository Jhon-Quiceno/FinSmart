package com.smartfinance.backend.servicios.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload to register (or update) an Expo push token for one of the current user's devices.
 *
 * @param expoPushToken the token Expo's SDK issued for this device/install
 * @param deviceId      a stable identifier for the device, used to upsert instead of
 *                       accumulating duplicate rows across reinstalls/token rotations — see
 *                       {@code uk_push_tokens_user_device} in {@code V26__create_push_tokens.sql}
 */
public record PushTokenRequest(
        @NotBlank(message = "El campo expoPushToken es obligatorio")
        String expoPushToken,
        @NotBlank(message = "El campo deviceId es obligatorio")
        String deviceId
) {
}
