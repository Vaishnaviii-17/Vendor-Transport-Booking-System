
package com.vendortransport.booking.service;

import com.vendortransport.booking.dto.RegisterRequest;
import com.vendortransport.booking.dto.RegisterResponse;
import com.vendortransport.booking.dto.LoginRequest;
import com.vendortransport.booking.dto.LoginResponse;

import com.vendortransport.booking.entity.User;
import com.vendortransport.booking.entity.UserRole;
import com.vendortransport.booking.repository.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // ============================================
    // CUSTOMER REGISTRATION
    // POST /api/auth/register
    // ============================================

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        String name = request.name().trim();

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Check whether email already exists
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered"
            );
        }

        // Create customer
        User user = new User();

        user.setName(name);
        user.setEmail(email);

        // Securely hash password using BCrypt
        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );

        // Assign customer role
        user.setRole(UserRole.CUSTOMER);

        try {
            User savedUser = userRepository.saveAndFlush(user);

            return new RegisterResponse(
                    savedUser.getId(),
                    savedUser.getName(),
                    savedUser.getEmail(),
                    savedUser.getRole().name(),
                    "Customer registered successfully"
            );

        } catch (DataIntegrityViolationException ex) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Registration could not be completed"
            );
        }
    }

    // ============================================
    // CUSTOMER LOGIN
    // POST /api/auth/login
    // ============================================

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid email or password"
                        )
                );

        // Verify password against stored BCrypt hash
        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        );

        if (!passwordMatches) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        // Generate signed JWT access token
        String token = jwtService.generateToken(user);

        // Return token and customer information
        return new LoginResponse(
                token,
                "Bearer",
                JwtService.EXPIRATION_SECONDS,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}
