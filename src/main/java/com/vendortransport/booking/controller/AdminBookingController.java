package com.vendortransport.booking.controller;

import com.vendortransport.booking.dto.AdminBookingRequest;
import com.vendortransport.booking.dto.AdminCustomerResponse;
import com.vendortransport.booking.dto.BookingResponse;
import com.vendortransport.booking.entity.BookingStatus;
import com.vendortransport.booking.entity.UserRole;
import com.vendortransport.booking.repository.UserRepository;
import com.vendortransport.booking.service.BookingService;
import com.vendortransport.booking.service.BookingStatusService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin/bookings")
public class AdminBookingController {

    private final BookingStatusService bookingStatusService;
    private final BookingService bookingService;
    private final UserRepository userRepository;

    public AdminBookingController(
            BookingStatusService bookingStatusService,
            BookingService bookingService,
            UserRepository userRepository
    ) {
        this.bookingStatusService = bookingStatusService;
        this.bookingService = bookingService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<BookingResponse> getAllBookings(
            @RequestParam(required = false) BookingStatus status
    ) {
        return bookingStatusService.getAllBookings(status);
    }

    @GetMapping("/customers")
    public List<AdminCustomerResponse> getCustomers() {
        return userRepository.findAll()
                .stream()
                .filter(user -> user.getRole() == UserRole.CUSTOMER)
                .map(AdminCustomerResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(
            @Valid @RequestBody AdminBookingRequest request
    ) {
        return bookingService.createAdminBooking(request);
    }

    @PatchMapping("/{id}/confirm")
    public BookingResponse confirmBooking(@PathVariable("id") Long id) {
        return bookingStatusService.confirmBooking(id);
    }

    @PatchMapping("/{id}/reject")
    public BookingResponse rejectBooking(@PathVariable("id") Long id) {
        return bookingStatusService.rejectBooking(id);
    }
}