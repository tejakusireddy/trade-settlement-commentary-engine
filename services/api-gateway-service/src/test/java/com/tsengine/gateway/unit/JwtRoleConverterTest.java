package com.tsengine.gateway.unit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tsengine.gateway.config.KeycloakProperties;
import com.tsengine.gateway.security.JwtRoleConverter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class JwtRoleConverterTest {

    @Test
    void testExtractRealmRoles() {
        JwtRoleConverter converter = converter();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user-1")
                .claim("realm_access", Map.of("roles", List.of("ops-user", "admin")))
                .build();

        JwtAuthenticationToken token = (JwtAuthenticationToken) converter.convert(jwt);
        Set<String> authorities = token.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertTrue(authorities.contains("ROLE_ops-user"));
        assertTrue(authorities.contains("ROLE_admin"));
    }

    @Test
    void testExtractResourceRoles() {
        JwtRoleConverter converter = converter();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user-1")
                .claim("resource_access", Map.of(
                        "trade-api", Map.of("roles", List.of("compliance-officer"))
                ))
                .build();

        JwtAuthenticationToken token = (JwtAuthenticationToken) converter.convert(jwt);
        Set<String> authorities = token.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertTrue(authorities.contains("ROLE_compliance-officer"));
    }

    @Test
    void testEmptyRoles() {
        JwtRoleConverter converter = converter();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user-1")
                .build();

        JwtAuthenticationToken token = (JwtAuthenticationToken) converter.convert(jwt);
        assertTrue(token.getAuthorities().isEmpty());
    }

    private JwtRoleConverter converter() {
        KeycloakProperties properties = new KeycloakProperties();
        properties.setClientId("trade-api");
        return new JwtRoleConverter(properties);
    }
}
