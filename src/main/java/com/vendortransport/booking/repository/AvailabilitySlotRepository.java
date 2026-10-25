
package com.vendortransport.booking.repository;

import com.vendortransport.booking.entity.AvailabilitySlot;
import com.vendortransport.booking.entity.AvailabilityStatus;
import com.vendortransport.booking.entity.VehicleType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AvailabilitySlotRepository
        extends JpaRepository<AvailabilitySlot, Long> {

    @Query("""
        SELECT s FROM AvailabilitySlot s
        JOIN FETCH s.vehicle v
        WHERE s.status = :status
          AND v.active = true
        ORDER BY s.availableDate, s.startTime
        """)
    List<AvailabilitySlot> findAvailableSlots(
            @Param("status") AvailabilityStatus status
    );

    @Query("""
        SELECT s FROM AvailabilitySlot s
        JOIN FETCH s.vehicle v
        WHERE s.status = :status
          AND v.active = true
          AND s.availableDate = :date
        ORDER BY s.availableDate, s.startTime
        """)
    List<AvailabilitySlot> findAvailableSlotsByDate(
            @Param("status") AvailabilityStatus status,
            @Param("date") LocalDate date
    );

    @Query("""
        SELECT s FROM AvailabilitySlot s
        JOIN FETCH s.vehicle v
        WHERE s.status = :status
          AND v.active = true
          AND v.vehicleType = :vehicleType
        ORDER BY s.availableDate, s.startTime
        """)
    List<AvailabilitySlot> findAvailableSlotsByVehicleType(
            @Param("status") AvailabilityStatus status,
            @Param("vehicleType") VehicleType vehicleType
    );

    @Query("""
        SELECT s FROM AvailabilitySlot s
        JOIN FETCH s.vehicle v
        WHERE s.status = :status
          AND v.active = true
          AND s.availableDate = :date
          AND v.vehicleType = :vehicleType
        ORDER BY s.availableDate, s.startTime
        """)
    List<AvailabilitySlot> findAvailableSlotsByDateAndVehicleType(
            @Param("status") AvailabilityStatus status,
            @Param("date") LocalDate date,
            @Param("vehicleType") VehicleType vehicleType
    );
}
