
package com.vendortransport.booking.service;

import com.vendortransport.booking.dto.AvailabilityResponse;
import com.vendortransport.booking.dto.VehicleResponse;
import com.vendortransport.booking.entity.AvailabilityStatus;
import com.vendortransport.booking.entity.VehicleType;
import com.vendortransport.booking.repository.AvailabilitySlotRepository;
import com.vendortransport.booking.repository.VehicleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class VehicleAvailabilityService {

    private final VehicleRepository vehicleRepository;
    private final AvailabilitySlotRepository slotRepository;

    public VehicleAvailabilityService(
            VehicleRepository vehicleRepository,
            AvailabilitySlotRepository slotRepository
    ) {
        this.vehicleRepository = vehicleRepository;
        this.slotRepository = slotRepository;
    }

    public List<VehicleResponse> getVehicles(VehicleType vehicleType) {

        if (vehicleType == null) {
            return vehicleRepository.findByActiveTrue()
                    .stream()
                    .map(VehicleResponse::from)
                    .toList();
        }

        return vehicleRepository
                .findByVehicleTypeAndActiveTrue(vehicleType)
                .stream()
                .map(VehicleResponse::from)
                .toList();
    }

    public List<AvailabilityResponse> getAvailability(
            LocalDate date,
            VehicleType vehicleType
    ) {

        AvailabilityStatus status = AvailabilityStatus.AVAILABLE;

        if (date != null && vehicleType != null) {
            return slotRepository
                    .findAvailableSlotsByDateAndVehicleType(
                            status, date, vehicleType
                    )
                    .stream()
                    .map(AvailabilityResponse::from)
                    .toList();
        }

        if (date != null) {
            return slotRepository
                    .findAvailableSlotsByDate(status, date)
                    .stream()
                    .map(AvailabilityResponse::from)
                    .toList();
        }

        if (vehicleType != null) {
            return slotRepository
                    .findAvailableSlotsByVehicleType(
                            status, vehicleType
                    )
                    .stream()
                    .map(AvailabilityResponse::from)
                    .toList();
        }

        return slotRepository.findAvailableSlots(status)
                .stream()
                .map(AvailabilityResponse::from)
                .toList();
    }
}
