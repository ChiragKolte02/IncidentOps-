
package com.example.incident_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String COOKIE_NAME = "INCIDENTOPS_TOKEN";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractToken(request);

        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.extractAllClaims(token);

            String username = claims.getSubject();
            Object userIdClaim = claims.get("userId");
            String role = claims.get("role", String.class);
            String team = claims.get("team", String.class);

            if (username != null
                    && userIdClaim != null
                    && role != null
                    && team != null
                    && SecurityContextHolder.getContext()
                    .getAuthentication() == null) {

                Long userId = Long.valueOf(userIdClaim.toString());

                if (!role.equals("ADMIN") && !role.equals("TEAM_MEMBER")) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                if (role.equals("TEAM_MEMBER")
                        && !team.equals("UNIX")
                        && !team.equals("DBA")) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                IncidentUserPrincipal principal =
                        new IncidentUserPrincipal(
                                userId, username, role, team);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority(
                                        "ROLE_" + role))
                        );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}
