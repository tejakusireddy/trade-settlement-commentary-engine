package com.tsengine.gateway.security;

import com.tsengine.gateway.config.KeycloakProperties;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final KeycloakProperties keycloakProperties;

    public JwtRoleConverter(KeycloakProperties keycloakProperties) {
        this.keycloakProperties = keycloakProperties;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<String> roles = new HashSet<>();
        extractRealmRoles(jwt, roles);
        extractResourceRoles(jwt, roles);

        Collection<GrantedAuthority> authorities = roles.stream()
                .map(role -> "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    @SuppressWarnings("unchecked")
    private void extractRealmRoles(Jwt jwt, Set<String> roles) {
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (!(realmAccess instanceof Map<?, ?> realmMap)) {
            return;
        }
        Object realmRoles = realmMap.get("roles");
        if (realmRoles instanceof List<?> list) {
            list.stream().map(String::valueOf).forEach(roles::add);
        }
    }

    @SuppressWarnings("unchecked")
    private void extractResourceRoles(Jwt jwt, Set<String> roles) {
        Object resourceAccess = jwt.getClaims().get("resource_access");
        if (!(resourceAccess instanceof Map<?, ?> resourceMap)) {
            return;
        }
        Object clientEntry = resourceMap.get(keycloakProperties.getClientId());
        if (!(clientEntry instanceof Map<?, ?> clientMap)) {
            return;
        }
        Object clientRoles = clientMap.get("roles");
        if (clientRoles instanceof List<?> list) {
            list.stream().map(String::valueOf).forEach(roles::add);
        }
    }
}
