package com.vendortransport.booking.repository;

import com.vendortransport.booking.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository
        extends JpaRepository<Vehicle, Long> {
}