package com.vendortransport.booking.dto;

import com.vendortransport.booking.entity.Vehicle;
import com.vendortransport.booking.entity.VehicleType;

public record AdminVehicleResponse(
        Long vehicleId,
        String registrationNumber,
        VehicleType vehicleType,
        Integer capacity,
        Boolean active
) {
    public static AdminVehicleResponse from(Vehicle vehicle) {
        return new AdminVehicleResponse(
                vehicle.getId(),
                vehicle.getRegistrationNumber(),
                vehicle.getVehicleType(),
                vehicle.getCapacity(),
                vehicle.getActive()
        );
    }
}