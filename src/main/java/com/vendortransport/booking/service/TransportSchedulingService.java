package com.vendortransport.booking.service;

import com.vendortransport.booking.dto.AdminVehicleResponse;
import com.vendortransport.booking.dto.TransportScheduleRequest;
import com.vendortransport.booking.dto.TransportSlotResponse;
import com.vendortransport.booking.entity.TransportSlot;
import com.vendortransport.booking.entity.Vehicle;
import com.vendortransport.booking.repository.TransportSlotRepository;
import com.vendortransport.booking.repository.VehicleRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransportSchedulingService {

    private final VehicleRepository vehicleRepository;
    private final TransportSlotRepository slotRepository;

    public TransportSchedulingService(
            VehicleRepository vehicleRepository,
            TransportSlotRepository slotRepository
    ) {
        this.vehicleRepository = vehicleRepository;
        this.slotRepository = slotRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminVehicleResponse> findAvailableVehicles(
            com.vendortransport.booking.entity.VehicleType vehicleType,
            Integer minimumCapacity,
            LocalDateTime departureTime
    ) {
        if (vehicleType == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vehicle type is required"
            );
        }

        if (minimumCapacity == null || minimumCapacity < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Minimum capacity must be at least 1"
            );
        }

        if (departureTime == null
                || !departureTime.isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Departure time must be in the future"
            );
        }

        return vehicleRepository
                .findByActiveTrueAndVehicleTypeAndCapacityGreaterThanEqualOrderByCapacityAsc(
                        vehicleType,
                        minimumCapacity
                )
                .stream()
                .filter(vehicle ->
                        !slotRepository.existsByVehicle_IdAndDepartureTime(
                                vehicle.getId(),
                                departureTime
                        )
                )
                .map(AdminVehicleResponse::from)
                .toList();
    }

    @Transactional
    public TransportSlotResponse scheduleTransport(
            TransportScheduleRequest request
    ) {
        LocalDateTime now = LocalDateTime.now();

        if (!request.departureTime().isAfter(now)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Departure time must be in the future"
            );
        }

        String origin = request.origin().trim();
        String destination = request.destination().trim();

        if (origin.equalsIgnoreCase(destination)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Origin and destination must be different"
            );
        }

        Vehicle vehicle = vehicleRepository
                .findById(request.vehicleId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Selected vehicle was not found"
                ));

        if (!Boolean.TRUE.equals(vehicle.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Selected vehicle is inactive"
            );
        }

        if (vehicle.getVehicleType() != request.vehicleType()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selected vehicle does not match the requested type"
            );
        }

        if (vehicle.getCapacity() == null
                || vehicle.getCapacity() < request.minimumCapacity()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selected vehicle does not meet the minimum capacity"
            );
        }

        if (slotRepository.existsByVehicle_IdAndDepartureTime(
                vehicle.getId(),
                request.departureTime())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This vehicle already has a journey at that departure time"
            );
        }

        TransportSlot slot = new TransportSlot();
        slot.setVehicle(vehicle);
        slot.setOrigin(origin);
        slot.setDestination(destination);
        slot.setDepartureTime(request.departureTime());

        // Initially, all seats are available to customers.
        slot.setAvailableSeats(vehicle.getCapacity());

        TransportSlot saved = slotRepository.saveAndFlush(slot);

        return TransportSlotResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<TransportSlotResponse> getScheduledTransport() {
        return slotRepository.findAllByOrderByDepartureTimeAsc()
                .stream()
                .map(TransportSlotResponse::from)
                .toList();
    }
}