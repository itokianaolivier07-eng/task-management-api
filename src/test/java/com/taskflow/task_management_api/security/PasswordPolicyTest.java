package com.taskflow.task_management_api.security;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTest {

    @Test
    void acceptsStrongPassword() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("Correct-Horse-42x", "a@b.com", "Alice"));
    }

    @Test
    void rejectsTooShort() {
        assertThrows(ResponseStatusException.class, () -> PasswordPolicy.validate("abc123", "a@b.com", "Alice"));
    }

    @Test
    void rejectsMissingDigitOrLetter() {
        assertThrows(ResponseStatusException.class, () -> PasswordPolicy.validate("OnlyLettersHereAbc", "a@b.com", "Alice"));
        assertThrows(ResponseStatusException.class, () -> PasswordPolicy.validate("123456789012345", "a@b.com", "Alice"));
    }

    @Test
    void rejectsCommonOrRepetitive() {
        assertThrows(ResponseStatusException.class, () -> PasswordPolicy.validate("Password1234", "a@b.com", "Alice"));
        assertThrows(ResponseStatusException.class, () -> PasswordPolicy.validate("aaaaaaaaaaaa1", "a@b.com", "Alice"));
    }

    @Test
    void rejectsPasswordContainingEmailName() {
        assertThrows(ResponseStatusException.class,
                () -> PasswordPolicy.validate("jonathan-2024-xyz", "jonathan@x.com", "J"));
    }
}
