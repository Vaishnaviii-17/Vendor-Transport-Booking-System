
package com.vendortransport.booking.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    @GetMapping("/profile")
    public Map<String, String> profile(
            @AuthenticationPrincipal Jwt jwt) {

        return Map.of(
            "email", jwt.getSubject(),
            "role", jwt.getClaimAsString("role"),
            "message", "Protected customer profile accessed"
        );
    }
}
