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
 * Limite le nombre de tentatives sur les routes sensibles (fenêtre fixe, en mémoire).
 * Pour les routes de connexion / vérification, seuls les ÉCHECS (réponse 4xx/5xx) comptent : un bureau ou un
 * réseau partagé ne se bloque pas à force de connexions réussies. Pour l'inscription et les envois d'e-mails,
 * chaque requête compte (freine la création massive de comptes et le spam).
 * Les compteurs sont locaux au processus : à déplacer vers un stockage partagé en déploiement multi-instances.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String MESSAGE = "Trop de tentatives, veuillez réessayer plus tard";
    private static final int MAX_TRACKED_CLIENTS = 10_000;
    private static final long QUARTER_HOUR = 15 * 60_000L;
    private static final long HOUR = 60 * 60_000L;

    /** failuresOnly = true : seules les réponses d'erreur comptent. */
    private record Rule(int maxAttempts, long windowMillis, boolean failuresOnly) {}

    private static final Map<String, Rule> RULES = Map.of(
            "/api/auth/login", new Rule(10, QUARTER_HOUR, true),
            "/api/auth/register", new Rule(5, HOUR, false),
            "/api/auth/verify-2fa", new Rule(10, QUARTER_HOUR, true),
            "/api/2fa/enable", new Rule(10, QUARTER_HOUR, true),
            "/api/2fa/disable", new Rule(10, QUARTER_HOUR, true),
            "/api/auth/verify-email", new Rule(20, QUARTER_HOUR, true),
            "/api/auth/resend-verification", new Rule(5, HOUR, false),
            "/api/auth/forgot-password", new Rule(5, HOUR, false),
            "/api/auth/reset-password", new Rule(10, QUARTER_HOUR, true),
            "/api/users/me/password", new Rule(10, QUARTER_HOUR, true));

    private record Window(long expiresAt, int attempts) {}

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Rule rule = RULES.get(request.getRequestURI());
        if (rule == null || !"POST".equals(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        String key = key(request);
        if (rule.failuresOnly()) {
            if (isBlocked(key, rule)) {
                reject(response);
                return;
            }
            chain.doFilter(request, response);
            if (response.getStatus() >= 400) {
                record(key, rule);
            }
            return;
        }
        if (!record(key, rule)) {
            reject(response);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isBlocked(String key, Rule rule) {
        Window window = windows.get(key);
        return window != null
                && window.expiresAt() >= System.currentTimeMillis()
                && window.attempts() >= rule.maxAttempts();
    }

    /** Enregistre une tentative ; renvoie false si la limite est dépassée. */
    private boolean record(String key, Rule rule) {
        long now = System.currentTimeMillis();
        if (windows.size() > MAX_TRACKED_CLIENTS) {
            windows.values().removeIf(w -> w.expiresAt() < now);
        }
        Window window = windows.compute(key, (k, current) ->
                current == null || current.expiresAt() < now
                        ? new Window(now + rule.windowMillis(), 1)
                        : new Window(current.expiresAt(), current.attempts() + 1));
        return window.attempts() <= rule.maxAttempts();
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now()
                + "\",\"status\":" + HttpStatus.TOO_MANY_REQUESTS.value()
                + ",\"message\":\"" + MESSAGE + "\"}");
    }

    private String key(HttpServletRequest request) {
        return request.getRemoteAddr() + "|" + request.getRequestURI();
    }
}
