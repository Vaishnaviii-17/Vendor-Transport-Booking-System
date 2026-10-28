package com.vendortransport.booking.service;

import com.vendortransport.booking.dto.TransportSlotResponse;
import com.vendortransport.booking.entity.VehicleType;
import com.vendortransport.booking.repository.TransportSlotRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TransportSearchService {

    private final TransportSlotRepository repository;

    public TransportSearchService(TransportSlotRepository repository) {
        this.repository = repository;
    }

    public List<TransportSlotResponse> search(
            String origin,
            String destination,
            LocalDate date,
            VehicleType vehicleType,
            Integer seats
    ) {
        if (seats != null && seats < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Seats must be at least 1"
            );
        }

        String normalizedOrigin = normalize(origin);
        String normalizedDestination = normalize(destination);

        LocalDateTime startTime =
                date == null ? null : date.atStartOfDay();

        LocalDateTime endTime =
                date == null ? null : date.plusDays(1).atStartOfDay();

        return repository.searchAvailableTransport(
                LocalDateTime.now(),
                normalizedOrigin,
                normalizedDestination,
                vehicleType,
                startTime,
                endTime,
                seats == null ? 1 : seats
        ).stream()
         .map(TransportSlotResponse::from)
         .toList();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}