
package com.vendortransport.booking.service;

import com.vendortransport.booking.entity.*;
import com.vendortransport.booking.repository.AvailabilitySlotRepository;
import com.vendortransport.booking.repository.VehicleRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleAvailabilityServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private AvailabilitySlotRepository slotRepository;

    private VehicleAvailabilityService service;

    @BeforeEach
    void setup() {
        service = new VehicleAvailabilityService(
                vehicleRepository,
                slotRepository
        );
    }

    @Test
    void shouldReturnActiveVehicles() {

        Vehicle car = createVehicle();

        when(vehicleRepository.findByActiveTrue())
                .thenReturn(List.of(car));

        var result = service.getVehicles(null);

        assertEquals(1, result.size());
        assertEquals("MH04AB1234",
                result.get(0).registrationNumber());

        verify(vehicleRepository).findByActiveTrue();
    }

    @Test
    void shouldFilterVehiclesByType() {

        Vehicle car = createVehicle();

        when(vehicleRepository.findByVehicleTypeAndActiveTrue(
                VehicleType.CAR
        )).thenReturn(List.of(car));

        var result = service.getVehicles(VehicleType.CAR);

        assertEquals(1, result.size());
        assertEquals(VehicleType.CAR,
                result.get(0).vehicleType());
    }

    @Test
    void shouldReturnAvailableSlots() {

        when(slotRepository.findAvailableSlots(
                AvailabilityStatus.AVAILABLE
        )).thenReturn(List.of());

        var result = service.getAvailability(null, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFilterAvailabilityByDate() {

        LocalDate date = LocalDate.of(2026, 10, 26);

        when(slotRepository.findAvailableSlotsByDate(
                AvailabilityStatus.AVAILABLE,
                date
        )).thenReturn(List.of());

        var result = service.getAvailability(date, null);

        assertTrue(result.isEmpty());

        verify(slotRepository).findAvailableSlotsByDate(
                AvailabilityStatus.AVAILABLE,
                date
        );
    }

    @Test
    void shouldFilterAvailabilityByDateAndVehicleType() {

        LocalDate date = LocalDate.of(2026, 10, 26);

        when(slotRepository.findAvailableSlotsByDateAndVehicleType(
                AvailabilityStatus.AVAILABLE,
                date,
                VehicleType.CAR
        )).thenReturn(List.of());

        var result = service.getAvailability(
                date, VehicleType.CAR
        );

        assertTrue(result.isEmpty());
    }

    private Vehicle createVehicle() {

        Vehicle vehicle = new Vehicle();

        vehicle.setId(1L);
        vehicle.setRegistrationNumber("MH04AB1234");
        vehicle.setVehicleType(VehicleType.CAR);
        vehicle.setCapacity(4);
        vehicle.setActive(true);

        return vehicle;
    }
}
