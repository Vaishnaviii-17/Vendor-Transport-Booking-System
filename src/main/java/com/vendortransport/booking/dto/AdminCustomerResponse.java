package com.vendortransport.booking.dto;

import com.vendortransport.booking.entity.User;

public record AdminCustomerResponse(
        Long id,
        String name,
        String email
) {
    public static AdminCustomerResponse from(User user) {
        return new AdminCustomerResponse(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }
}