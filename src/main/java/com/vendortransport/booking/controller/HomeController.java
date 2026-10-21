package com.vendortransport.booking.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/api")
    public Map<String, String> home() {
        return Map.of(
            "application", "Vendor Transport Booking System",
            "version", "1.0.0",
            "status", "running",
            "message", "Backend API is working successfully"
        );
    }
}