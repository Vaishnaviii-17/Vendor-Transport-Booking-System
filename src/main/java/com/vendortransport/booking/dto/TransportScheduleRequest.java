package com.vendortransport.booking.dto;

import com.vendortransport.booking.entity.VehicleType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TransportScheduleRequest(
        @NotBlank(message = "Origin is required")
        String origin,

        @NotBlank(message = "Destination is required")
        String destination,

        @NotNull(message = "Departure time is required")
        LocalDateTime departureTime,

        @NotNull(message = "Vehicle type is required")
        VehicleType vehicleType,

        @NotNull(message = "Minimum capacity is required")
        @Min(value = 1, message = "Minimum capacity must be at least 1")
        Integer minimumCapacity,

        @NotNull(message = "Please select a vehicle")
        Long vehicleId
) {}