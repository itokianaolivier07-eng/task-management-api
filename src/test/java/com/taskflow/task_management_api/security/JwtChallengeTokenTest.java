package com.taskflow.task_management_api.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtChallengeTokenTest {

    private static final String SECRET = "ZGV2LW9ubHktc2VjcmV0LWNoYW5nZS1tZS1pbi1wcm9kdWN0aW9uLTAxMjM0NTY3ODk=";
    private final JwtService jwt = new JwtService(SECRET, 60_000, "task-management-api");

    @Test
    void challengeTokenCannotBeUsedAsSessionToken() {
        String challenge = jwt.generateChallengeToken("a@b.com");
        assertThrows(JwtException.class, () -> jwt.extractEmail(challenge));
        assertEquals("a@b.com", jwt.extractChallengeEmail(challenge));
    }

    @Test
    void sessionTokenCannotBeUsedAsChallengeToken() {
        String session = jwt.generateToken("a@b.com");
        assertThrows(JwtException.class, () -> jwt.extractChallengeEmail(session));
    }
}
