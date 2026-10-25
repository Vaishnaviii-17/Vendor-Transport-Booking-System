
package com.vendortransport.booking.dto;

import com.vendortransport.booking.entity.AvailabilitySlot;
import com.vendortransport.booking.entity.AvailabilityStatus;
import com.vendortransport.booking.entity.VehicleType;

import java.time.LocalDate;
import java.time.LocalTime;

public record AvailabilityResponse(
        Long slotId,
        Long vehicleId,
        String registrationNumber,
        VehicleType vehicleType,
        Integer capacity,
        LocalDate availableDate,
        LocalTime startTime,
        LocalTime endTime,
        AvailabilityStatus status
) {
    public static AvailabilityResponse from(AvailabilitySlot slot) {
        return new AvailabilityResponse(
                slot.getId(),
                slot.getVehicle().getId(),
                slot.getVehicle().getRegistrationNumber(),
                slot.getVehicle().getVehicleType(),
                slot.getVehicle().getCapacity(),
                slot.getAvailableDate(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getStatus()
        );
    }
}
