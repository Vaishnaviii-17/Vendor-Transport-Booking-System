
package com.vendortransport.booking.dto;

public record LoginResponse(
    String token,
    String tokenType,
    long expiresIn,
    Long userId,
    String name,
    String email,
    String role
) {}
