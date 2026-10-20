package org.cabbook.auth;

import jakarta.validation.constraints.*;
import org.cabbook.user.Role;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email String email,
            @Size(max = 20) String phone,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotNull Role role,                       // RIDER or DRIVER (ADMIN not allowed)
            String vehicleModel,                      // required when role = DRIVER
            String vehicleNumber) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record AuthResponse(String token, Long userId, String name, Role role) {}
}
