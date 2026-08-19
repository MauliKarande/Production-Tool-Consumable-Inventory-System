package com.ameya.inventory.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class UserDtos {

    public record CreateRequest(
            @NotBlank(message = "Username is required") String username,
            @NotBlank(message = "Password is required") @Size(min = 8, message = "Password must be at least 8 characters") String password,
            Long employeeId,
            @NotBlank(message = "Role is required") String roleName
    ) {
    }

    public record UpdateRequest(
            Long employeeId,
            @NotBlank(message = "Role is required") String roleName,
            @NotNull(message = "Active flag is required") Boolean active
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "Current password is required") String currentPassword,
            @NotBlank(message = "New password is required") @Size(min = 8, message = "Password must be at least 8 characters") String newPassword
    ) {
    }

    public record ResetPasswordRequest(
            @NotBlank(message = "New password is required") @Size(min = 8, message = "Password must be at least 8 characters") String newPassword
    ) {
    }

    public record Response(
            Long id,
            String username,
            Long employeeId,
            String employeeName,
            String roleName,
            boolean active,
            Instant lastLoginAt
    ) {
    }
}
