package com.vendortransport.booking.service;

import com.vendortransport.booking.dto.RegisterRequest;
import com.vendortransport.booking.dto.RegisterResponse;
import com.vendortransport.booking.entity.User;
import com.vendortransport.booking.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private AuthService authService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        authService = new AuthService(
            userRepository,
            new BCryptPasswordEncoder()
        );
    }

    @Test
    void shouldRegisterCustomerSuccessfully() {

        when(userRepository.existsByEmail(anyString()))
            .thenReturn(false);

        when(userRepository.saveAndFlush(any(User.class)))
            .thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                return user;
            });

        RegisterResponse response = authService.register(
            new RegisterRequest(
                "Test Customer",
                "customer@example.com",
                "SecurePass123!"
            )
        );

        assertEquals("CUSTOMER", response.role());
        assertEquals("customer@example.com", response.email());

        verify(userRepository).saveAndFlush(argThat(user ->
            !user.getPasswordHash().equals("SecurePass123!")
            && user.getPasswordHash().startsWith("$2")
        ));
    }

    @Test
    void shouldRejectDuplicateEmail() {

        when(userRepository.existsByEmail(anyString()))
            .thenReturn(true);

        assertThrows(
            ResponseStatusException.class,
            () -> authService.register(
                new RegisterRequest(
                    "Test Customer",
                    "customer@example.com",
                    "SecurePass123!"
                )
            )
        );

        verify(userRepository, never()).saveAndFlush(any());
    }
}