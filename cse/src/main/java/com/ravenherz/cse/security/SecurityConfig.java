package com.ravenherz.cse.security;

import com.ravenherz.cse.controller.AuthSupport;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.header.writers.ContentSecurityPolicyHeaderWriter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.web.multipart.support.MultipartFilter;

/**
 * CSRF, frame options, nosniff, HSTS on HTTPS, and a site CSP.
 * No {@code requiresChannel} (servlet {@code CONFIDENTIAL} stays the HTTPS redirector).
 * {@code /apps/**} is excluded from CSP so {@code .cseapp} HTML can use its own origins.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthSupport authSupport;
    private final CapabilityService capabilityService;

    public SecurityConfig(AuthSupport authSupport, CapabilityService capabilityService) {
        this.authSupport = authSupport;
        this.capabilityService = capabilityService;
    }

    @Bean
    public FilterRegistrationBean<MultipartFilter> multipartFilterBeforeCsrf() {
        FilterRegistrationBean<MultipartFilter> registration = new FilterRegistrationBean<>(new MultipartFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(new CseCookieCsrfTokenRepository())
                        .csrfTokenRequestHandler(new CseCsrfTokenRequestHandler()))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .headers(headers -> {
                    headers.defaultsDisabled();
                    headers.frameOptions(frame -> frame.deny());
                    headers.contentTypeOptions(Customizer.withDefaults());
                    headers.httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)
                            .maxAgeInSeconds(31536000));
                    headers.addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                            new NegatedRequestMatcher(PathPatternRequestMatcher.withDefaults()
                                    .matcher("/apps/**")),
                            new ContentSecurityPolicyHeaderWriter(CseContentSecurityPolicy.DIRECTIVES)));
                })
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new CseAuthenticationEntryPoint())
                        .accessDeniedHandler(new CseAccessDeniedHandler()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/editor", "/editor/**")
                        .access(new EditorAccessAuthorizationManager(capabilityService))
                        .anyRequest().permitAll())
                .addFilterAfter(new CseCookieAuthenticationFilter(authSupport),
                        SecurityContextHolderFilter.class)
                .addFilterAfter(new AppCapabilityFilter(authSupport, capabilityService),
                        CseCookieAuthenticationFilter.class)
                .addFilterAfter(new SiteCapabilityFilter(authSupport, capabilityService),
                        AppCapabilityFilter.class)
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class);
        return http.build();
    }
}
