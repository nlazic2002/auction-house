package com.example.auction_house.configuration;

import com.example.auction_house.service.AccountService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.HeaderWriterLogoutHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;


@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = new CookieCsrfTokenRepository();
        repository.setCookieCustomizer(cookie -> cookie.sameSite("Lax"));
        return repository;
    }

    @Bean
    public SecretKey jwtSigningKey(@Value("${security.jwt.secret}") String secret) {
        byte[] key = Base64.getDecoder().decode(secret);
        if (key.length < 32) {
            throw new IllegalArgumentException("security.jwt.secret must contain at least 32 Base64-encoded bytes");
        }
        return new SecretKeySpec(key, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return NimbusJwtEncoder.withSecretKey(jwtSigningKey).algorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public JwtDecoder jwtDecoder(
            SecretKey jwtSigningKey,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.audience}") String audience
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(issuer),
                        new JwtClaimValidator<List<String>>(
                                "aud",
                                audiences -> audiences != null && audiences.contains(audience)
                        ),
                        new JwtClaimValidator<Instant>(
                                "exp",
                                expiration -> expiration != null
                        ),
                        new JwtClaimValidator<String>(
                                "sub",
                                subject -> subject != null && !subject.isBlank()
                        )
                )
        );
        return decoder;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtDecoder jwtDecoder, AccountService accountService) {
        return new JwtAuthenticationFilter(jwtDecoder, accountService);
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(
            final AccountService accountService,
            final PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(accountService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public ProviderManager authenticationManager(final DaoAuthenticationProvider daoAuthenticationProvider) {
        return new ProviderManager(daoAuthenticationProvider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            final HttpSecurity http,
            final AuthenticationManager authenticationManager,
            final JwtAuthenticationFilter jwtAuthenticationFilter,
            final CookieCsrfTokenRepository csrfTokenRepository
    ) {
        RequestCache nullRequestCache = new NullRequestCache();
        HeaderWriterLogoutHandler clearSiteData = new HeaderWriterLogoutHandler(
                new ClearSiteDataHeaderWriter(ClearSiteDataHeaderWriter.Directive.ALL)
        );

        http
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout((logout) -> {
                    logout.logoutUrl("/authentication/logout");
                    logout.addLogoutHandler(clearSiteData);
                    logout.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler());

                })
                .requestCache((cache) -> cache
                        .requestCache(nullRequestCache)
                )
                .headers((headers) -> {
                    /*
                     * use default HTTP headers but,
                     * allow X-Frame-Options on same origin because of h2-console
                     */
                    headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin);
                })
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationManager(authenticationManager)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                                .accessDeniedHandler((request, response, exception) -> response.setStatus(403))
                )
                .authorizeHttpRequests((authorize) -> {
                    authorize
                            .requestMatchers("/h2-console/**", "/authentication/**").permitAll()
                            .anyRequest().authenticated();
                });

        return http.build();
    }
}
