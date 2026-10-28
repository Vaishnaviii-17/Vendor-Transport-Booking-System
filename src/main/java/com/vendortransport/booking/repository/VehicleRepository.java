package com.vendortransport.booking.repository;

import com.vendortransport.booking.entity.Vehicle;
import com.vendortransport.booking.entity.VehicleType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByActiveTrue();

    List<Vehicle> findByVehicleTypeAndActiveTrue(
            VehicleType vehicleType
    );

    List<Vehicle>
    findByActiveTrueAndVehicleTypeAndCapacityGreaterThanEqualOrderByCapacityAsc(
            VehicleType vehicleType,
            Integer capacity
    );

    boolean existsByRegistrationNumber(String registrationNumber);
}