package com.vendortransport.booking.dto;

import com.vendortransport.booking.entity.TransportSlot;
import com.vendortransport.booking.entity.VehicleType;

import java.time.LocalDateTime;

public record TransportSlotResponse(
        Long slotId,
        Long vehicleId,
        String registrationNumber,
        VehicleType vehicleType,
        Integer vehicleCapacity,
        String origin,
        String destination,
        LocalDateTime departureTime,
        Integer availableSeats
) {
    public static TransportSlotResponse from(TransportSlot slot) {
        var vehicle = slot.getVehicle();

        return new TransportSlotResponse(
                slot.getId(),
                vehicle.getId(),
                vehicle.getRegistrationNumber(),
                vehicle.getVehicleType(),
                vehicle.getCapacity(),
                slot.getOrigin(),
                slot.getDestination(),
                slot.getDepartureTime(),
                slot.getAvailableSeats()
        );
    }
}