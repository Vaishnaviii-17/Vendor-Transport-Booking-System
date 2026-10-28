
package com.vendortransport.booking.controller;

import com.vendortransport.booking.dto.AvailabilityResponse;
import com.vendortransport.booking.dto.VehicleResponse;
import com.vendortransport.booking.entity.VehicleType;
import com.vendortransport.booking.service.VehicleAvailabilityService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
public class VehicleAvailabilityController {

    private final VehicleAvailabilityService service;

    public VehicleAvailabilityController(
            VehicleAvailabilityService service
    ) {
        this.service = service;
    }

    @GetMapping("/api/vehicles")
    public List<VehicleResponse> getVehicles(
            @RequestParam(required = false) VehicleType vehicleType
    ) {
        return service.getVehicles(vehicleType);
    }

    @GetMapping("/api/availability")
    public List<AvailabilityResponse> getAvailability(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @RequestParam(required = false)
            VehicleType vehicleType
    ) {
        return service.getAvailability(date, vehicleType);
    }
}
