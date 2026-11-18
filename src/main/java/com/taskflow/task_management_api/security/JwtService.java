package com.taskflow.task_management_api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private static final String TYPE_CLAIM = "typ";
    private static final String TWO_FACTOR = "2fa";
    private static final long CHALLENGE_TTL_MS = 5 * 60_000L;

    private final SecretKey key;
    private final long expiration;
    private final String issuer;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration}") long expiration,
                      @Value("${jwt.issuer}") String issuer) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expiration = expiration;
        this.issuer = issuer;
    }

    /** Token de session (accès à l'API). */
    public String generateToken(String email) {
        return build(email, expiration, null);
    }

    /** Jeton de 5 min délivré après le mot de passe, échangeable contre une session avec le code 2FA. */
    public String generateChallengeToken(String email) {
        return build(email, CHALLENGE_TTL_MS, TWO_FACTOR);
    }

    /** Refuse les jetons de défi : ils ne donnent pas accès à l'API. */
    public String extractEmail(String token) {
        Claims claims = parse(token);
        if (claims.get(TYPE_CLAIM) != null) {
            throw new JwtException("Type de token invalide");
        }
        return claims.getSubject();
    }

    public String extractChallengeEmail(String token) {
        Claims claims = parse(token);
        if (!TWO_FACTOR.equals(claims.get(TYPE_CLAIM))) {
            throw new JwtException("Type de token invalide");
        }
        return claims.getSubject();
    }

    public record Session(String email, Instant issuedAt) {}

    /** Comme extractEmail, mais renvoie aussi la date d'émission (pour refuser les sessions révoquées). */
    public Session extractSession(String token) {
        Claims claims = parse(token);
        if (claims.get(TYPE_CLAIM) != null) {
            throw new JwtException("Type de token invalide");
        }
        Date issuedAt = claims.getIssuedAt();
        return new Session(claims.getSubject(), issuedAt == null ? Instant.EPOCH : issuedAt.toInstant());
    }

    private String build(String email, long ttlMillis, String type) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(email)
                .issuer(issuer)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMillis));
        if (type != null) builder.claim(TYPE_CLAIM, type);
        return builder.signWith(key).compact();
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).requireIssuer(issuer).build()
                .parseSignedClaims(token).getPayload();
    }
}
