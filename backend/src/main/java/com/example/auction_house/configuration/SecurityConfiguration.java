package com.example.auction_house.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HeaderWriterLogoutHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;


@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(
            final HttpSecurity http
    ) {
        RequestCache nullRequestCache = new NullRequestCache();
        HeaderWriterLogoutHandler clearSiteData = new HeaderWriterLogoutHandler(new ClearSiteDataHeaderWriter(ClearSiteDataHeaderWriter.Directive.ALL));

        http
                //TODO: implement csrf protection endpoint
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout((logout) -> {
                    logout.logoutSuccessUrl("/authentication/logout");
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
                .authorizeHttpRequests((authorize) -> {
                    authorize
                            .requestMatchers("/h2-console/**", "/authentication/**").permitAll()
                            .anyRequest().authenticated();
                });

        return http.build();
    }
}
