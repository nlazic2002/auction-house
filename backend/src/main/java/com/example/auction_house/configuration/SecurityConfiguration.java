package com.example.auction_house.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(
            final HttpSecurity http
    ) {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .headers((headers) -> {
                    /*
                     * use default HTTP headers but,
                     * allow X-Frame-Options on same origin because of h2-console
                     */
                    headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin);
                })
                .authorizeHttpRequests((authorize) -> {
                    authorize
                            .requestMatchers("/h2-console/**").permitAll()
                            .anyRequest().authenticated();
                });

        return http.build();
    }
}
