package com.taskflow.task_management_api.security;

import com.taskflow.task_management_api.entity.User;
import com.taskflow.task_management_api.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * Lit le JWT dans l'en-tête « Authorization: Bearer » (clients API) ou dans le cookie HttpOnly (navigateur).
 * Défense CSRF : une requête qui modifie des données et s'authentifie par cookie doit porter l'en-tête
 * « X-Requested-With » (un site tiers ne peut pas l'ajouter sans passer par le CORS, qui est restreint).
 * Un token émis avant User.sessionsValidAfter (mot de passe changé, compte supprimé) est refusé.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String COOKIE_NAME = "taskflow_session";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = null;
        boolean fromCookie = false;
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        } else if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    token = cookie.getValue();
                    fromCookie = true;
                }
            }
        }

        boolean csrfOk = !fromCookie
                || SAFE_METHODS.contains(request.getMethod())
                || request.getHeader("X-Requested-With") != null;

        if (token != null && !token.isBlank() && csrfOk) {
            try {
                JwtService.Session session = jwtService.extractSession(token);
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    userRepository.findByEmail(session.email())
                            .filter(user -> !isRevoked(user, session))
                            .ifPresent(user -> {
                                UserDetails details = org.springframework.security.core.userdetails.User
                                        .withUsername(user.getEmail()).password(user.getPassword())
                                        .roles("USER").build();
                                SecurityContextHolder.getContext().setAuthentication(
                                        new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
                            });
                }
            } catch (JwtException | IllegalArgumentException e) {
                // token invalide : la requête reste non authentifiée
            }
        }
        chain.doFilter(request, response);
    }

    private static boolean isRevoked(User user, JwtService.Session session) {
        return user.getSessionsValidAfter() != null
                && session.issuedAt().isBefore(user.getSessionsValidAfter().truncatedTo(ChronoUnit.SECONDS));
    }
}
