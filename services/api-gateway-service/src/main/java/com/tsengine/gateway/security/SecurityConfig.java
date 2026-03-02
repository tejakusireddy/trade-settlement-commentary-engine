package com.tsengine.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsengine.common.ApiResponse;
import com.tsengine.gateway.audit.AuditLogFilter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtRoleConverter jwtRoleConverter;
    private final RateLimitFilter rateLimitFilter;
    private final AuditLogFilter auditLogFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(
            JwtRoleConverter jwtRoleConverter,
            RateLimitFilter rateLimitFilter,
            AuditLogFilter auditLogFilter,
            ObjectMapper objectMapper
    ) {
        this.jwtRoleConverter = jwtRoleConverter;
        this.rateLimitFilter = rateLimitFilter;
        this.auditLogFilter = auditLogFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(objectMapper.writeValueAsString(
                                    ApiResponse.error("Unauthorized")
                            ));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(objectMapper.writeValueAsString(
                                    ApiResponse.error("Forbidden")
                            ));
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/prometheus").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/trades/**")
                        .hasAnyRole("ops-user", "compliance-officer", "admin")
                        .requestMatchers(HttpMethod.POST, "/api/v1/trades/**")
                        .hasAnyRole("ops-user", "compliance-officer", "admin")
                        .requestMatchers(HttpMethod.GET, "/api/v1/breaches/**")
                        .hasAnyRole("ops-user", "compliance-officer", "admin")
                        .requestMatchers(HttpMethod.GET, "/api/v1/commentaries/**")
                        .hasAnyRole("ops-user", "compliance-officer", "admin")
                        .requestMatchers(HttpMethod.POST, "/api/v1/commentaries/{id}/approve")
                        .hasAnyRole("compliance-officer", "admin")
                        .requestMatchers(HttpMethod.GET, "/api/v1/ai/**").hasRole("admin")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtRoleConverter)))
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(auditLogFilter, RateLimitFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
