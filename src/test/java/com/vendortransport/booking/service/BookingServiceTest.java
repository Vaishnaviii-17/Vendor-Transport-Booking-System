
package com.vendortransport.booking.service;

import com.vendortransport.booking.dto.BookingRequest;
import com.vendortransport.booking.dto.BookingResponse;
import com.vendortransport.booking.entity.*;
import com.vendortransport.booking.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private TransportSlotRepository slotRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    private User customer;
    private TransportSlot slot;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId(1L);
        customer.setEmail("customer@example.com");
        customer.setRole(UserRole.CUSTOMER);

        slot = new TransportSlot();
        slot.setId(1L);
        slot.setOrigin("Thane Station");
        slot.setDestination("Mumbai Airport");
        slot.setDepartureTime(
                LocalDateTime.now().plusDays(10)
        );
        slot.setAvailableSeats(4);
    }

    @Test
    void shouldCreateBookingSuccessfully() {

        when(userRepository.findByEmail(
                "customer@example.com"))
                .thenReturn(Optional.of(customer));

        when(slotRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(slot));

        when(bookingRepository.saveAndFlush(any(Booking.class)))
                .thenAnswer(invocation -> {
                    Booking booking = invocation.getArgument(0);
                    booking.setId(100L);
                    booking.prePersist();
                    return booking;
                });

        BookingResponse response = bookingService.createBooking(
                "customer@example.com",
                new BookingRequest(1L, 2)
        );

        assertEquals(100L, response.bookingId());
        assertEquals(BookingStatus.PENDING, response.status());
        assertEquals(2, response.seatsRequested());
        assertEquals(2, slot.getAvailableSeats());

        verify(bookingRepository).saveAndFlush(any(Booking.class));
    }

    @Test
    void shouldRejectInsufficientSeats() {

        when(userRepository.findByEmail(
                "customer@example.com"))
                .thenReturn(Optional.of(customer));

        when(slotRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(slot));

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class,
                        () -> bookingService.createBooking(
                                "customer@example.com",
                                new BookingRequest(1L, 5)
                        ));

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        assertEquals(4, slot.getAvailableSeats());

        verify(bookingRepository, never())
                .saveAndFlush(any(Booking.class));
    }

    @Test
    void shouldRejectInvalidSlot() {

        when(userRepository.findByEmail(
                "customer@example.com"))
                .thenReturn(Optional.of(customer));

        when(slotRepository.findByIdForUpdate(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class,
                        () -> bookingService.createBooking(
                                "customer@example.com",
                                new BookingRequest(999L, 1)
                        ));

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        verify(bookingRepository, never())
                .saveAndFlush(any(Booking.class));
    }

    @Test
    void shouldRejectZeroSeats() {

        when(userRepository.findByEmail(
                "customer@example.com"))
                .thenReturn(Optional.of(customer));

        when(slotRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(slot));

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class,
                        () -> bookingService.createBooking(
                                "customer@example.com",
                                new BookingRequest(1L, 0)
                        ));

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        assertEquals(4, slot.getAvailableSeats());
    }

    @Test
    void shouldRejectPastDeparture() {

        slot.setDepartureTime(
                LocalDateTime.now().minusDays(1)
        );

        when(userRepository.findByEmail(
                "customer@example.com"))
                .thenReturn(Optional.of(customer));

        when(slotRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(slot));

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class,
                        () -> bookingService.createBooking(
                                "customer@example.com",
                                new BookingRequest(1L, 1)
                        ));

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        assertEquals(4, slot.getAvailableSeats());
    }

    @Test
    void shouldRejectUnknownCustomer() {

        when(userRepository.findByEmail(
                "unknown@example.com"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class,
                        () -> bookingService.createBooking(
                                "unknown@example.com",
                                new BookingRequest(1L, 1)
                        ));

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatusCode()
        );

        verifyNoInteractions(slotRepository);
        verifyNoInteractions(bookingRepository);
    }
}
