
package com.vendortransport.booking.service;

import com.vendortransport.booking.dto.BookingResponse;
import com.vendortransport.booking.entity.*;
import com.vendortransport.booking.repository.BookingRepository;
import com.vendortransport.booking.repository.TransportSlotRepository;
import com.vendortransport.booking.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingStatusServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private TransportSlotRepository slotRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingStatusService bookingStatusService;

    private Booking booking;
    private TransportSlot slot;
    private User customer;

    @BeforeEach
    void setUp() {

        customer = new User();
        customer.setId(1L);
        customer.setName("Test Customer");
        customer.setEmail("customer@example.com");
        customer.setRole(UserRole.CUSTOMER);

        slot = new TransportSlot();
        slot.setId(1L);
        slot.setOrigin("Thane Station");
        slot.setDestination("Mumbai Airport");
        slot.setDepartureTime(LocalDateTime.now().plusDays(10));
        slot.setAvailableSeats(2);

        booking = new Booking();
        booking.setId(1L);
        booking.setUser(customer);
        booking.setSlot(slot);
        booking.setSeatsRequested(2);
        booking.setStatus(BookingStatus.PENDING);
    }

    // Test 1: Confirm a pending booking
    @Test
    void shouldConfirmPendingBooking() {

        when(bookingRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(booking));

        when(bookingRepository.saveAndFlush(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response =
                bookingStatusService.confirmBooking(1L);

        assertEquals(
                BookingStatus.CONFIRMED,
                response.status()
        );

        assertEquals(2, slot.getAvailableSeats());

        verify(bookingRepository).saveAndFlush(booking);
        verifyNoInteractions(slotRepository);
    }

    // Test 2: Reject a pending booking and restore reserved seats
    @Test
    void shouldRejectPendingBookingAndRestoreSeats() {

        // Initial booking lookup
        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking));

        // Lock transport slot first
        when(slotRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(slot));

        // Lock booking after transport slot
        when(bookingRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(booking));

        when(bookingRepository.saveAndFlush(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response =
                bookingStatusService.rejectBooking(1L);

        assertEquals(
                BookingStatus.REJECTED,
                response.status()
        );

        // Seats should be restored from 2 to 4
        assertEquals(4, slot.getAvailableSeats());

        verify(slotRepository).save(slot);
        verify(bookingRepository).saveAndFlush(booking);

        // Verify the expected repository call order
        InOrder inOrder = inOrder(
                bookingRepository,
                slotRepository
        );

        inOrder.verify(bookingRepository).findById(1L);
        inOrder.verify(slotRepository).findByIdForUpdate(1L);
        inOrder.verify(bookingRepository).findByIdForUpdate(1L);
        inOrder.verify(slotRepository).save(slot);
        inOrder.verify(bookingRepository).saveAndFlush(booking);
    }

    // Test 3: Prevent duplicate confirmation
    @Test
    void shouldPreventDuplicateConfirmation() {

        booking.setStatus(BookingStatus.CONFIRMED);

        when(bookingRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(booking));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingStatusService.confirmBooking(1L)
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verify(bookingRepository, never()).saveAndFlush(any());
        verifyNoInteractions(slotRepository);
    }

    // Test 4: Prevent duplicate rejection
    @Test
    void shouldPreventDuplicateRejection() {

        booking.setStatus(BookingStatus.REJECTED);

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking));

        when(slotRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(slot));

        when(bookingRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(booking));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingStatusService.rejectBooking(1L)
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        // Rejected bookings must not restore seats again
        assertEquals(2, slot.getAvailableSeats());

        verify(slotRepository, never()).save(any());
        verify(bookingRepository, never()).saveAndFlush(any());
    }

    // Test 5: Missing booking returns 404
    @Test
    void shouldReturnNotFoundForMissingBooking() {

        when(bookingRepository.findByIdForUpdate(99L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingStatusService.confirmBooking(99L)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(bookingRepository, never()).saveAndFlush(any());
    }

    // Test 6: Retrieve bookings belonging to a customer
    @Test
    void shouldRetrieveCustomerBookings() {

        when(userRepository.findByEmail("customer@example.com"))
                .thenReturn(Optional.of(customer));

        when(bookingRepository.findByUserId(1L))
                .thenReturn(List.of(booking));

        List<BookingResponse> responses =
                bookingStatusService.getMyBookings(
                        "customer@example.com"
                );

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).bookingId());
        assertEquals(1L, responses.get(0).customerId());
    }

    // Test 7: Prevent customers from viewing others' bookings
    @Test
    void shouldPreventAccessToAnotherCustomersBooking() {

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingStatusService.getBookingById(
                        1L,
                        "another@example.com",
                        false
                )
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );
    }

    // Test 8: Allow administrators to view booking details
    @Test
    void shouldAllowAdminToViewBooking() {

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking));

        BookingResponse response =
                bookingStatusService.getBookingById(
                        1L,
                        "admin@example.com",
                        true
                );

        assertEquals(1L, response.bookingId());
        assertEquals(1L, response.customerId());
    }

    // Test 9: Filter bookings by status
    @Test
    void shouldFilterBookingsByStatus() {

        when(bookingRepository.findByStatus(BookingStatus.PENDING))
                .thenReturn(List.of(booking));

        List<BookingResponse> responses =
                bookingStatusService.getAllBookings(
                        BookingStatus.PENDING
                );

        assertEquals(1, responses.size());

        assertEquals(
                BookingStatus.PENDING,
                responses.get(0).status()
        );

        verify(bookingRepository).findByStatus(
                BookingStatus.PENDING
        );
    }
}
