package com.vendortransport.booking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminBookingRequest(
        @NotBlank(message = "Customer email is required")
        @Email(message = "Enter a valid customer email")
        String customerEmail,

        @NotNull(message = "Slot ID is required")
        Long slotId,

        @NotNull(message = "Seats requested is required")
        @Min(value = 1, message = "At least one seat is required")
        Integer seatsRequested
) {}