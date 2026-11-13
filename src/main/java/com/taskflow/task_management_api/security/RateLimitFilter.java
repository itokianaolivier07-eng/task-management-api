package com.taskflow.task_management_api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite le nombre de tentatives sur les routes d'authentification (fenêtre fixe, en mémoire) :
 * freine le bourrage d'identifiants et la création massive de comptes.
 * Les compteurs sont locaux au processus : à déplacer vers un stockage partagé en déploiement multi-instances.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";
    private static final String MESSAGE = "Trop de tentatives, veuillez réessayer plus tard";
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private record Rule(int maxAttempts, long windowMillis) {}

    private static final Map<String, Rule> RULES = Map.of(
            LOGIN_PATH, new Rule(10, 15 * 60_000L),
            REGISTER_PATH, new Rule(5, 60 * 60_000L));

    private record Window(long expiresAt, int attempts) {}

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Rule rule = RULES.get(request.getRequestURI());
        if (rule == null || !"POST".equals(request.getMethod()) || isAllowed(request, rule)) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now()
                + "\",\"status\":" + HttpStatus.TOO_MANY_REQUESTS.value()
                + ",\"message\":\"" + MESSAGE + "\"}");
    }

    private boolean isAllowed(HttpServletRequest request, Rule rule) {
        long now = System.currentTimeMillis();
        if (windows.size() > MAX_TRACKED_CLIENTS) {
            windows.values().removeIf(w -> w.expiresAt() < now);
        }
        Window window = windows.compute(key(request), (k, current) ->
                current == null || current.expiresAt() < now
                        ? new Window(now + rule.windowMillis(), 1)
                        : new Window(current.expiresAt(), current.attempts() + 1));
        return window.attempts() <= rule.maxAttempts();
    }

    private String key(HttpServletRequest request) {
        return request.getRemoteAddr() + "|" + request.getRequestURI();
    }
}
