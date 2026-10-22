
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

    // Customer Registration
    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        String name = request.name().trim();

        String email = request.email().trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Email is already registered"
            );
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);

        user.setPasswordHash(
            passwordEncoder.encode(request.password())
        );

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

    // Customer Login with JWT Generation
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        String email = request.email().trim()
                .toLowerCase(Locale.ROOT);

        // Find registered customer
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password"
            ));

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

        // Generate signed JWT after successful authentication
        String token = jwtService.generateToken(user);

        return new LoginResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole().name(),
            "Login successful",
            token,
            "Bearer",
            jwtService.getExpiresInSeconds()
        );
    }
}
