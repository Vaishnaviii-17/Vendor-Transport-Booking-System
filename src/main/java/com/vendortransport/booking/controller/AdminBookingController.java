
package com.vendortransport.booking.controller;

import com.vendortransport.booking.dto.BookingResponse;
import com.vendortransport.booking.entity.BookingStatus;
import com.vendortransport.booking.service.BookingStatusService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/bookings")
public class AdminBookingController {

    private final BookingStatusService bookingStatusService;

    public AdminBookingController(
            BookingStatusService bookingStatusService
    ) {
        this.bookingStatusService = bookingStatusService;
    }

    @GetMapping
    public List<BookingResponse> getAllBookings(
            @RequestParam(required = false) BookingStatus status
    ) {
        return bookingStatusService.getAllBookings(status);
    }

    @PatchMapping("/{id}/confirm")
    public BookingResponse confirmBooking(@PathVariable Long id) {
        return bookingStatusService.confirmBooking(id);
    }

    @PatchMapping("/{id}/reject")
    public BookingResponse rejectBooking(@PathVariable Long id) {
        return bookingStatusService.rejectBooking(id);
    }
}
