package com.courshare.identity.config;

import com.courshare.identity.infrastructure.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String gatewayUserId = request.getHeader("X-User-Id");
        String gatewayRolesHeader = request.getHeader("X-User-Roles");

        if (gatewayUserId != null && !gatewayUserId.isBlank()) {
            List<SimpleGrantedAuthority> authorities;
            if (gatewayRolesHeader != null && !gatewayRolesHeader.isBlank()) {
                authorities = java.util.Arrays.stream(gatewayRolesHeader.split(","))
                        .map(String::trim)
                        .filter(role -> !role.isBlank())
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();
            } else {
                authorities = java.util.Collections.emptyList();
            }
            var authentication = new UsernamePasswordAuthenticationToken(
                    gatewayUserId,
                    null,
                    authorities
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                try {
                    Claims claims = jwtService.parseToken(token);
                    if (jwtService.isAccessToken(claims)) {
                        @SuppressWarnings("unchecked")
                        List<String> roles = claims.get(JwtService.CLAIM_ROLES, List.class);
                        var authorities = roles.stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                .toList();
                        var authentication = new UsernamePasswordAuthenticationToken(
                                claims.getSubject(),
                                null,
                                authorities
                        );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (JwtException ignored) {
                    SecurityContextHolder.clearContext();
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
