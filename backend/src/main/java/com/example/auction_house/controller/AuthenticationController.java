package com.example.auction_house.controller;

import com.example.auction_house.dto.request.LoginRequestDto;
import com.example.auction_house.dto.response.CsrfResponseDto;
import com.example.auction_house.dto.response.LoginResponseDto;
import com.example.auction_house.model.Account;
import com.example.auction_house.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/authentication")
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthenticationController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequestDto) {

        var authentication = authenticationManager
                .authenticate(
                    UsernamePasswordAuthenticationToken
                            .unauthenticated(
                                    loginRequestDto.email(),
                                    loginRequestDto.password()
                            )
                );

        Jwt jwt = jwtService.generateToken((Account) authentication.getPrincipal());

        assert jwt.getIssuedAt() != null;

        LoginResponseDto response = new LoginResponseDto(
                jwt.getTokenValue(),
                "Bearer",
                Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).toSeconds()
        );

        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfResponseDto> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new CsrfResponseDto(csrfToken.getHeaderName(), csrfToken.getToken()));
    }
}
