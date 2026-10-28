package com.vendortransport.booking.controller;

import com.vendortransport.booking.dto.TransportSlotResponse;
import com.vendortransport.booking.entity.VehicleType;
import com.vendortransport.booking.service.TransportSearchService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transport-slots")
public class TransportSearchController {

    private final TransportSearchService service;

    public TransportSearchController(TransportSearchService service) {
        this.service = service;
    }

    @GetMapping
    public List<TransportSlotResponse> searchTransport(
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @RequestParam(required = false)
            VehicleType vehicleType,

            @RequestParam(required = false)
            Integer seats
    ) {
        return service.search(
                origin,
                destination,
                date,
                vehicleType,
                seats
        );
    }
}