
package com.vendortransport.booking.controller;

import com.vendortransport.booking.dto.BookingRequest;
import com.vendortransport.booking.dto.BookingResponse;
import com.vendortransport.booking.service.BookingService;
import com.vendortransport.booking.service.BookingStatusService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final BookingStatusService bookingStatusService;

    public BookingController(
            BookingService bookingService,
            BookingStatusService bookingStatusService
    ) {
        this.bookingService = bookingService;
        this.bookingStatusService = bookingStatusService;
    }

    // Task 8: Create a new booking request
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {

        BookingResponse response = bookingService.createBooking(
                jwt.getSubject(),
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Task 9: View all bookings belonging to the logged-in customer
    @GetMapping("/my")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            @AuthenticationPrincipal Jwt jwt
    ) {

        List<BookingResponse> bookings =
                bookingStatusService.getMyBookings(
                        jwt.getSubject()
                );

        return ResponseEntity.ok(bookings);
    }

    // Task 9: View booking details by booking ID
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {

        boolean isAdmin = "ADMIN".equals(
                jwt.getClaimAsString("role")
        );

        BookingResponse response =
                bookingStatusService.getBookingById(
                        id,
                        jwt.getSubject(),
                        isAdmin
                );

        return ResponseEntity.ok(response);
    }
}
