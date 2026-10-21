package com.vendortransport.booking.repository;

import com.vendortransport.booking.entity.TransportSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransportSlotRepository
        extends JpaRepository<TransportSlot, Long> {

    List<TransportSlot> findByDepartureTimeAfter(
            LocalDateTime dateTime);
}