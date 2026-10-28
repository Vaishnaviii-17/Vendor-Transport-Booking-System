package com.vendortransport.booking.repository;

import com.vendortransport.booking.entity.TransportSlot;
import com.vendortransport.booking.entity.VehicleType;

import java.time.LocalDateTime;
import java.util.List;

public interface TransportSlotRepositoryCustom {

    List<TransportSlot> searchAvailableTransport(
            LocalDateTime now,
            String origin,
            String destination,
            VehicleType vehicleType,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer seats
    );
}