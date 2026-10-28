package com.vendortransport.booking.repository;

import com.vendortransport.booking.entity.TransportSlot;
import com.vendortransport.booking.entity.Vehicle;
import com.vendortransport.booking.entity.VehicleType;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Repository
public class TransportSlotRepositoryCustomImpl
        implements TransportSlotRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<TransportSlot> searchAvailableTransport(
            LocalDateTime now,
            String origin,
            String destination,
            VehicleType vehicleType,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer seats
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<TransportSlot> query =
                cb.createQuery(TransportSlot.class);

        Root<TransportSlot> slot = query.from(TransportSlot.class);

        Join<TransportSlot, Vehicle> vehicle =
                slot.join("vehicle", JoinType.INNER);

        List<Predicate> predicates = new ArrayList<>();

        // Only future journeys.
        predicates.add(
                cb.greaterThan(slot.get("departureTime"), now)
        );

        // Enough seats must be available.
        predicates.add(
                cb.greaterThanOrEqualTo(
                        slot.get("availableSeats"),
                        seats
                )
        );

        // Exclude inactive vehicles.
        predicates.add(
                cb.isTrue(vehicle.get("active"))
        );

        // Add origin filter only when supplied.
        if (origin != null && !origin.isBlank()) {
            predicates.add(
                    cb.like(
                            cb.lower(slot.get("origin")),
                            "%" + origin.trim().toLowerCase(Locale.ROOT) + "%"
                    )
            );
        }

        // Add destination filter only when supplied.
        if (destination != null && !destination.isBlank()) {
            predicates.add(
                    cb.like(
                            cb.lower(slot.get("destination")),
                            "%" + destination.trim().toLowerCase(Locale.ROOT) + "%"
                    )
            );
        }

        // Add vehicle type filter only when supplied.
        if (vehicleType != null) {
            predicates.add(
                    cb.equal(vehicle.get("vehicleType"), vehicleType)
            );
        }

        // Start of the selected date, inclusive.
        if (startTime != null) {
            predicates.add(
                    cb.greaterThanOrEqualTo(
                            slot.get("departureTime"),
                            startTime
                    )
            );
        }

        // Start of the next date, exclusive.
        if (endTime != null) {
            predicates.add(
                    cb.lessThan(
                            slot.get("departureTime"),
                            endTime
                    )
            );
        }

        query.select(slot)
                .distinct(true)
                .where(predicates.toArray(new Predicate[0]))
                .orderBy(cb.asc(slot.get("departureTime")));

        return entityManager.createQuery(query).getResultList();
    }
}