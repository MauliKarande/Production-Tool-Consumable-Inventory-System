package com.ameya.inventory.dto.auth;

public record LoginResponse(
        String token,
        String username,
        String role,
        Long userId
) {
}
