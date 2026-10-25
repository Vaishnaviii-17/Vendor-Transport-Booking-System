
package com.vendortransport.booking.service;

import com.vendortransport.booking.entity.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    public static final long EXPIRATION_SECONDS = 3600;

    private final JwtEncoder jwtEncoder;

    public JwtService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public String generateToken(User user) {

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("vendor-transport-booking")
            .issuedAt(now)
            .expiresAt(
                now.plus(EXPIRATION_SECONDS, ChronoUnit.SECONDS)
            )
            .subject(user.getEmail())
            .claim("userId", user.getId())
            .claim("role", user.getRole().name())
            .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
            .build();

        return jwtEncoder.encode(
            JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }
}
