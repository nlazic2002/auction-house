package com.example.auction_house.service.impl;

import com.example.auction_house.model.Account;
import com.example.auction_house.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JwtServiceImpl implements JwtService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final String audience;
    private final Duration accessTokenTtl;

    public JwtServiceImpl(
            JwtEncoder jwtEncoder,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.audience}") String audience,
            @Value("${security.jwt.access-token-ttl}") Duration accessTokenTtl
    ) {
        Assert.isTrue(accessTokenTtl.toSeconds() > 0, "JWT access token lifetime must be at least one second");
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.audience = audience;
        this.accessTokenTtl = accessTokenTtl;
    }

    @Override
    public Jwt generateToken(Account account) {
        Assert.notNull(account, "Account is required");
        Assert.isTrue(account.getId() > 0, "Account must have a persisted ID");

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .subject(account.getEmail())
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenTtl))
                .id(UUID.randomUUID().toString())
                .claim("role", account.getRole())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims));
    }
}
