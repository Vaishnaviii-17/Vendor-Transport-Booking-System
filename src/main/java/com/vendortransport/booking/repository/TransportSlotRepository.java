
package com.vendortransport.booking.repository;

import com.vendortransport.booking.entity.TransportSlot;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransportSlotRepository
        extends JpaRepository<TransportSlot, Long> {

    List<TransportSlot> findByDepartureTimeAfter(
            LocalDateTime dateTime
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s FROM TransportSlot s
        WHERE s.id = :slotId
        """)
    Optional<TransportSlot> findByIdForUpdate(
            @Param("slotId") Long slotId
    );
}
