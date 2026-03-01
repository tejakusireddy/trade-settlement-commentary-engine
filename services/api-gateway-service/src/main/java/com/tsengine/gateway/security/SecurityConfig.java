package com.tsengine.gateway.security;

import com.tsengine.gateway.audit.AuditLogFilter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
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
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtRoleConverter jwtRoleConverter;
    private final RateLimitFilter rateLimitFilter;
    private final AuditLogFilter auditLogFilter;

    public SecurityConfig(
            JwtRoleConverter jwtRoleConverter,
            RateLimitFilter rateLimitFilter,
            AuditLogFilter auditLogFilter
    ) {
        this.jwtRoleConverter = jwtRoleConverter;
        this.rateLimitFilter = rateLimitFilter;
        this.auditLogFilter = auditLogFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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
                        .requestMatchers(HttpMethod.POST, "/api/v1/commentaries/**/approve")
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
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
