package com.taskflow.task_management_api.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "ZGV2LW9ubHktc2VjcmV0LWNoYW5nZS1tZS1pbi1wcm9kdWN0aW9uLTAxMjM0NTY3ODk=";
    private static final String ISSUER = "task-management-api";

    @Test
    void tokenRoundTrip() {
        JwtService jwt = new JwtService(SECRET, 60_000, ISSUER);
        assertEquals("a@b.com", jwt.extractEmail(jwt.generateToken("a@b.com")));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwt = new JwtService(SECRET, -1_000, ISSUER);
        String token = jwt.generateToken("a@b.com");
        assertThrows(JwtException.class, () -> jwt.extractEmail(token));
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwt = new JwtService(SECRET, 60_000, ISSUER);
        String[] parts = jwt.generateToken("a@b.com").split("\\.");
        char first = parts[2].charAt(0);
        String badSignature = (first == 'A' ? 'B' : 'A') + parts[2].substring(1);
        String tampered = parts[0] + "." + parts[1] + "." + badSignature;

        assertThrows(JwtException.class, () -> jwt.extractEmail(tampered));
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() {
        JwtService other = new JwtService(SECRET, 60_000, "autre-service");
        String token = other.generateToken("a@b.com");

        assertThrows(JwtException.class, () -> new JwtService(SECRET, 60_000, ISSUER).extractEmail(token));
    }
}
