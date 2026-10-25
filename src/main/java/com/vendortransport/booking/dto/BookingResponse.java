
package com.vendortransport.booking.dto;

import com.vendortransport.booking.entity.Booking;
import com.vendortransport.booking.entity.BookingStatus;

import java.time.LocalDateTime;

public record BookingResponse(
        Long bookingId,
        Long slotId,
        Long customerId,
        String origin,
        String destination,
        LocalDateTime departureTime,
        Integer seatsRequested,
        BookingStatus status,
        LocalDateTime createdAt
) {
    public static BookingResponse from(Booking booking) {
        var slot = booking.getSlot();

        return new BookingResponse(
                booking.getId(),
                slot.getId(),
                booking.getUser().getId(),
                slot.getOrigin(),
                slot.getDestination(),
                slot.getDepartureTime(),
                booking.getSeatsRequested(),
                booking.getStatus(),
                booking.getCreatedAt()
        );
    }
}
