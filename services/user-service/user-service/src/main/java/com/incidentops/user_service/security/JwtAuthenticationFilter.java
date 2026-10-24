
package com.incidentops.user_service.security;

import com.incidentops.user_service.entity.User;
import com.incidentops.user_service.repository.UserRepository;
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
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        String token = extractToken(request);

        System.out.println("\n========== JWT FILTER ==========");
        System.out.println("Request: " + request.getMethod() + " " + requestUri);
        System.out.println("Authorization header present: "
                + (request.getHeader(HttpHeaders.AUTHORIZATION) != null));
        System.out.println("Token present: " + (token != null && !token.isBlank()));

        if (token == null || token.isBlank()) {
            System.out.println("No token found; continuing without JWT authentication.");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.extractAllClaims(token);
            String username = claims.getSubject();

            System.out.println("JWT subject: " + username);

            if (username != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                User user = userRepository.findByUsername(username)
                        .filter(User::isEnabled)
                        .orElse(null);

                System.out.println("User found and enabled: " + (user != null));

                if (user != null) {
                    String authority = "ROLE_" + user.getRole().name();

                    System.out.println("Authenticated username: " + user.getUsername());
                    System.out.println("Database role: " + user.getRole());
                    System.out.println("Database team: " + user.getTeam());
                    System.out.println("Granted authority: " + authority);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    user.getUsername(),
                                    null,
                                    List.of(new SimpleGrantedAuthority(authority))
                            );

                    authentication.setDetails(claims);

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);

                    System.out.println("SecurityContext authentication set successfully.");
                } else {
                    System.out.println(
                            "Authentication not set: user does not exist or is disabled."
                    );
                }
            } else if (username == null) {
                System.out.println("Authentication not set: JWT subject is null.");
            } else {
                System.out.println(
                        "Authentication already exists in SecurityContext."
                );
            }

        } catch (JwtException | IllegalArgumentException exception) {
            System.err.println(
                    "JWT authentication failed for " + requestUri
                            + ": " + exception.getClass().getSimpleName()
                            + " - " + exception.getMessage()
            );

            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);

        System.out.println("Response status for " + requestUri
                + ": " + response.getStatus());
        System.out.println("================================\n");
    }

    private String extractToken(HttpServletRequest request) {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith("Bearer ")) {
            System.out.println("Token source: Authorization Bearer header");
            return header.substring(7);
        }

        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    System.out.println("Token source: " + COOKIE_NAME + " cookie");
                    return cookie.getValue();
                }
            }
        }

        System.out.println("Token source: none");
        return null;
    }
}
