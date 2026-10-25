
package com.vendortransport.booking.service;

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

import java.util.List;

@Service
public class BookingStatusService {

    private final BookingRepository bookingRepository;
    private final TransportSlotRepository slotRepository;
    private final UserRepository userRepository;

    public BookingStatusService(
            BookingRepository bookingRepository,
            TransportSlotRepository slotRepository,
            UserRepository userRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
    }

    // Retrieve all bookings belonging to the logged-in customer
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(String email) {

        User customer = findUser(email);

        return bookingRepository.findByUserId(customer.getId())
                .stream()
                .map(BookingResponse::from)
                .toList();
    }

    // Retrieve booking details with ownership verification
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(
            Long bookingId,
            String email,
            boolean isAdmin
    ) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found"
                ));

        if (!isAdmin &&
                !booking.getUser().getEmail().equals(email)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Booking not found"
            );
        }

        return BookingResponse.from(booking);
    }

    // Retrieve all bookings or filter by booking status
    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings(
            BookingStatus status
    ) {

        List<Booking> bookings = status == null
                ? bookingRepository.findAll()
                : bookingRepository.findByStatus(status);

        return bookings.stream()
                .map(BookingResponse::from)
                .toList();
    }

    // Confirm a pending booking
    @Transactional
    public BookingResponse confirmBooking(Long bookingId) {

        Booking booking = bookingRepository
                .findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found"
                ));

        // Only pending bookings can be confirmed
        requirePending(booking);

        // Seats were already reserved during booking creation.
        // Do not deduct seats again.
        booking.setStatus(BookingStatus.CONFIRMED);

        Booking savedBooking =
                bookingRepository.saveAndFlush(booking);

        return BookingResponse.from(savedBooking);
    }

    // Reject a pending booking and restore its reserved seats
    @Transactional
    public BookingResponse rejectBooking(Long bookingId) {

        // Step 1: Read the booking to identify its transport slot.
        // This initial lookup does not request a pessimistic lock.
        Booking initialBooking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found"
                ));

        Long slotId = initialBooking.getSlot().getId();

        // Step 2: Lock the transport slot first.
        // This matches the lock order used by booking creation.
        TransportSlot slot = slotRepository
                .findByIdForUpdate(slotId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Transport slot not found"
                ));

        // Step 3: Lock the booking after locking the slot.
        Booking booking = bookingRepository
                .findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found"
                ));

        // Step 4: Validate the current status while holding the lock.
        // This prevents repeated rejection and duplicate seat restoration.
        requirePending(booking);

        // Step 5: Restore the seats reserved during booking creation.
        slot.setAvailableSeats(
                slot.getAvailableSeats() + booking.getSeatsRequested()
        );

        // Step 6: Mark the booking as rejected.
        booking.setStatus(BookingStatus.REJECTED);

        // Step 7: Persist both changes in the same transaction.
        slotRepository.save(slot);

        Booking savedBooking =
                bookingRepository.saveAndFlush(booking);

        return BookingResponse.from(savedBooking);
    }

    // Find the authenticated user using their email
    private User findUser(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"
                ));
    }

    // Validate booking status before confirmation or rejection
    private void requirePending(Booking booking) {

        if (booking.getStatus() != BookingStatus.PENDING) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only pending bookings can be updated"
            );
        }
    }
}
