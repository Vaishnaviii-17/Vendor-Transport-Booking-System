
package com.vendortransport.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BookingRequest(
        @NotNull(message = "Slot ID is required")
        Long slotId,

        @NotNull(message = "Seats requested is required")
        @Min(value = 1, message = "At least one seat is required")
        Integer seatsRequested
) {}
