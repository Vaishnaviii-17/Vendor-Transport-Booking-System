
package com.vendortransport.booking.service;

import com.vendortransport.booking.entity.User;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final long expirationMinutes;

    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${app.jwt.expiration-minutes:60}")
            long expirationMinutes) {

        if (expirationMinutes <= 0) {
            throw new IllegalArgumentException(
                "JWT expiration must be positive"
            );
        }

        this.jwtEncoder = jwtEncoder;
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(User user) {

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("vendor-transport-booking")
            .subject(user.getEmail())
            .issuedAt(now)
            .expiresAt(
                now.plus(Duration.ofMinutes(expirationMinutes))
            )
            .claim("userId", user.getId())
            .claim("role", user.getRole().name())
            .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
            .build();

        return jwtEncoder.encode(
            JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

    public long getExpiresInSeconds() {
        return Duration.ofMinutes(expirationMinutes).toSeconds();
    }
}
