package com.example.ebookstore.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * JWT authentication response.
 * Using @Getter + @Builder (not @Value) so @Builder.Default works correctly
 * for the constant 'type' field.
 */
@Getter
@Builder
public class AuthResponse {
    private final String token;

    /** Always "Bearer" — declared with @Builder.Default so the builder honours it. */
    @Builder.Default
    private final String type = "Bearer";

    private final Long   userId;
    private final String name;
    private final String email;
}
