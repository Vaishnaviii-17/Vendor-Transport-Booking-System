
package com.vendortransport.booking.dto;

import com.vendortransport.booking.entity.Vehicle;
import com.vendortransport.booking.entity.VehicleType;

public record VehicleResponse(
        Long id,
        String registrationNumber,
        VehicleType vehicleType,
        Integer capacity,
        Boolean active
) {
    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getRegistrationNumber(),
                vehicle.getVehicleType(),
                vehicle.getCapacity(),
                vehicle.getActive()
        );
    }
}
