
package com.vendortransport.booking.service;

import com.vendortransport.booking.dto.RegisterRequest;
import com.vendortransport.booking.dto.RegisterResponse;
import com.vendortransport.booking.dto.LoginRequest;
import com.vendortransport.booking.dto.LoginResponse;

import com.vendortransport.booking.entity.User;
import com.vendortransport.booking.entity.UserRole;
import com.vendortransport.booking.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    private BCryptPasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setup() {

        passwordEncoder = new BCryptPasswordEncoder();

        authService = new AuthService(
            userRepository,
            passwordEncoder,
            jwtService
        );
    }

    // TEST 1: Successful Customer Registration
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

    // TEST 2: Duplicate Email Registration
    @Test
    void shouldRejectDuplicateEmail() {

        when(userRepository.existsByEmail(anyString()))
            .thenReturn(true);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> authService.register(
                new RegisterRequest(
                    "Test Customer",
                    "customer@example.com",
                    "SecurePass123!"
                )
            )
        );

        assertEquals(
            HttpStatus.CONFLICT,
            exception.getStatusCode()
        );

        verify(userRepository, never())
            .saveAndFlush(any(User.class));
    }

    // TEST 3: Successful Customer Login with JWT
    @Test
    void shouldLoginSuccessfullyAndGenerateJwt() {

        User user = createTestUser();

        when(userRepository.findByEmail("customer@example.com"))
            .thenReturn(Optional.of(user));

        when(jwtService.generateToken(user))
            .thenReturn("mock.jwt.token");

        when(jwtService.getExpiresInSeconds())
            .thenReturn(3600L);

        LoginResponse response = authService.login(
            new LoginRequest(
                "customer@example.com",
                "SecurePass123!"
            )
        );

        assertEquals(1L, response.id());
        assertEquals("Test Customer", response.name());
        assertEquals("customer@example.com", response.email());
        assertEquals("CUSTOMER", response.role());
        assertEquals("Login successful", response.message());

        assertEquals("mock.jwt.token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600L, response.expiresIn());

        verify(jwtService, times(1))
            .generateToken(user);
    }

    // TEST 4: Incorrect Password
    @Test
    void shouldRejectIncorrectPassword() {

        User user = createTestUser();

        when(userRepository.findByEmail("customer@example.com"))
            .thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> authService.login(
                new LoginRequest(
                    "customer@example.com",
                    "WrongPassword123"
                )
            )
        );

        assertEquals(
            HttpStatus.UNAUTHORIZED,
            exception.getStatusCode()
        );

        verify(jwtService, never())
            .generateToken(any(User.class));
    }

    // TEST 5: Unregistered Email
    @Test
    void shouldRejectUnregisteredEmail() {

        when(userRepository.findByEmail("unknown@example.com"))
            .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> authService.login(
                new LoginRequest(
                    "unknown@example.com",
                    "SecurePass123!"
                )
            )
        );

        assertEquals(
            HttpStatus.UNAUTHORIZED,
            exception.getStatusCode()
        );

        verify(jwtService, never())
            .generateToken(any(User.class));
    }

    // Helper: Create a sample customer
    private User createTestUser() {

        User user = new User();

        user.setId(1L);
        user.setName("Test Customer");
        user.setEmail("customer@example.com");

        user.setPasswordHash(
            passwordEncoder.encode("SecurePass123!")
        );

        user.setRole(UserRole.CUSTOMER);

        return user;
    }
}
