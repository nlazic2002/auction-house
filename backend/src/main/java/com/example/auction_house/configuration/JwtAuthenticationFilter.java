package com.example.auction_house.configuration;

import com.example.auction_house.model.Account;
import com.example.auction_house.service.AccountService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;
    private final AccountService accountService;
    private final DefaultBearerTokenResolver tokenResolver = new DefaultBearerTokenResolver();
    private final BearerTokenAuthenticationEntryPoint entryPoint = new BearerTokenAuthenticationEntryPoint();
    private final AccountStatusUserDetailsChecker accountChecker = new AccountStatusUserDetailsChecker();

    public JwtAuthenticationFilter(JwtDecoder jwtDecoder, AccountService accountService) {
        this.jwtDecoder = jwtDecoder;
        this.accountService = accountService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String token = tokenResolver.resolve(request);

            if (token != null) {

                Jwt jwt = jwtDecoder.decode(token);
                Account account = accountService.loadUserByEmail(Objects.requireNonNull(jwt.getSubject()));
                accountChecker.check(account);
                account.eraseCredentials();

                var authentication =
                        UsernamePasswordAuthenticationToken
                        .authenticated(
                            account,
                                null,
                                account.getAuthorities()
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);

            }
        } catch (JwtException | AuthenticationException exception) {
            reject(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        entryPoint.commence(request, response, new InvalidBearerTokenException("Invalid bearer token"));
    }
}
