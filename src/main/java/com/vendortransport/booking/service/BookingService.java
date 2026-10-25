
package com.vendortransport.booking.service;

import com.vendortransport.booking.dto.BookingRequest;
import com.vendortransport.booking.dto.BookingResponse;
import com.vendortransport.booking.entity.Booking;
import com.vendortransport.booking.entity.BookingStatus;
import com.vendortransport.booking.entity.TransportSlot;
import com.vendortransport.booking.entity.User;
import com.vendortransport.booking.repository.BookingRepository;
import com.vendortransport.booking.repository.TransportSlotRepository;
import com.vendortransport.booking.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TransportSlotRepository slotRepository;
    private final UserRepository userRepository;

    public BookingService(
            BookingRepository bookingRepository,
            TransportSlotRepository slotRepository,
            UserRepository userRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BookingResponse createBooking(
            String customerEmail,
            BookingRequest request
    ) {

        User customer = userRepository
                .findByEmail(customerEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated customer not found"
                ));

        TransportSlot slot = slotRepository
                .findByIdForUpdate(request.slotId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Transport slot not found"
                ));

        if (slot.getDepartureTime()
                .isBefore(LocalDateTime.now())
                || slot.getDepartureTime()
                .isEqual(LocalDateTime.now())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot book a past or departed transport slot"
            );
        }

        if (request.seatsRequested() == null
                || request.seatsRequested() < 1) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Seats requested must be at least 1"
            );
        }

        if (slot.getAvailableSeats()
                < request.seatsRequested()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Insufficient seats available"
            );
        }

        Booking booking = new Booking();
        booking.setUser(customer);
        booking.setSlot(slot);
        booking.setSeatsRequested(request.seatsRequested());
        booking.setStatus(BookingStatus.PENDING);

        slot.setAvailableSeats(
                slot.getAvailableSeats() - request.seatsRequested()
        );

        slotRepository.save(slot);

        Booking savedBooking =
                bookingRepository.saveAndFlush(booking);

        return BookingResponse.from(savedBooking);
    }
}
