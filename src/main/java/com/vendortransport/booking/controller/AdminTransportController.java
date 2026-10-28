package com.vendortransport.booking.controller;

import com.vendortransport.booking.dto.AdminVehicleResponse;
import com.vendortransport.booking.dto.TransportScheduleRequest;
import com.vendortransport.booking.dto.TransportSlotResponse;
import com.vendortransport.booking.entity.VehicleType;
import com.vendortransport.booking.service.TransportSchedulingService;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminTransportController {

    private final TransportSchedulingService schedulingService;

    public AdminTransportController(
            TransportSchedulingService schedulingService
    ) {
        this.schedulingService = schedulingService;
    }

    @GetMapping("/vehicles/available")
    public List<AdminVehicleResponse> findAvailableVehicles(
            @RequestParam VehicleType vehicleType,
            @RequestParam Integer minimumCapacity,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime departureTime
    ) {
        return schedulingService.findAvailableVehicles(
                vehicleType,
                minimumCapacity,
                departureTime
        );
    }

    @PostMapping("/transport-slots")
    @ResponseStatus(HttpStatus.CREATED)
    public TransportSlotResponse scheduleTransport(
            @Valid @RequestBody TransportScheduleRequest request
    ) {
        return schedulingService.scheduleTransport(request);
    }

    @GetMapping("/transport-slots")
    public List<TransportSlotResponse> getScheduledTransport() {
        return schedulingService.getScheduledTransport();
    }
}