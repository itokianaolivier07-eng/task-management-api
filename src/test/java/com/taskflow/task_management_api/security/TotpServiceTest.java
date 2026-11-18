package com.taskflow.task_management_api.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TotpServiceTest {

    private final TotpService totp = new TotpService();

    @Test
    void matchesRfc6238TestVector() {
        // Secret RFC 6238 « 12345678901234567890 » en Base32, t = 59 s -> pas 1 -> 94287082 (8 chiffres) -> 287082
        assertEquals("287082", totp.code("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", 1));
    }

    @Test
    void currentCodeIsAcceptedAndWrongCodeRejected() {
        String secret = totp.newSecret();
        long step = System.currentTimeMillis() / 1000 / 30;
        assertTrue(totp.verify(secret, totp.code(secret, step)));
        String wrong = totp.code(secret, step + 10);
        assertFalse(totp.verify(secret, wrong));
        assertFalse(totp.verify(secret, "abc"));
        assertFalse(totp.verify(null, "123456"));
    }
}
