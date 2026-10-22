
package com.vendortransport.booking.dto;

public record LoginResponse(
    Long id,
    String name,
    String email,
    String role,
    String message,
    String accessToken,
    String tokenType,
    long expiresIn
) {}
