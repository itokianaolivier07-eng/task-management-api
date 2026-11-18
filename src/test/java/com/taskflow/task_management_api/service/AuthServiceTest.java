package com.taskflow.task_management_api.service;

import com.taskflow.task_management_api.dto.*;
import com.taskflow.task_management_api.entity.User;
import com.taskflow.task_management_api.repository.UserRepository;
import com.taskflow.task_management_api.security.JwtService;
import com.taskflow.task_management_api.security.TotpService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;
    @Mock TotpService totpService;
    @Mock AccountService accountService;
    @Mock NotificationService notificationService;
    @InjectMocks AuthService authService;

    private static User user(boolean verified) {
        return User.builder().name("Alice").email("a@b.com").password("x").emailVerified(verified).build();
    }

    @Test
    void registerCreatesUnverifiedUserAndSendsConfirmationEmail() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Str0ngPassword12")).thenReturn("hashed");

        MessageResponse response = authService.register(new RegisterRequest("Alice", "a@b.com", "Str0ngPassword12"));

        assertEquals(AuthService.REGISTER_MESSAGE, response.message());
        verify(userRepository).save(argThat((User u) -> "hashed".equals(u.getPassword()) && !u.isEmailVerified()));
        verify(accountService).sendVerification(any(User.class));
    }

    @Test
    void registerWithExistingEmailRevealsNothingAndCreatesNothing() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user(true)));

        MessageResponse response = authService.register(new RegisterRequest("Alice", "a@b.com", "Str0ngPassword12"));

        assertEquals(AuthService.REGISTER_MESSAGE, response.message());
        verify(userRepository, never()).save(any());
        verify(notificationService).send(eq("a@b.com"), anyString(), anyString());
    }

    @Test
    void registerRejectsWeakPassword() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register(new RegisterRequest("Alice", "a@b.com", "short1")));

        assertEquals(400, ex.getStatusCode().value());
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginWithBadCredentialsFails() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequest("a@b.com", "wrong")));
    }

    @Test
    void loginRefusesUnverifiedEmail() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user(false)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(new LoginRequest("a@b.com", "pw")));

        assertEquals(403, ex.getStatusCode().value());
        verify(jwtService, never()).generateToken(anyString());
    }

    @Test
    void loginReturnsTokenForVerifiedUser() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user(true)));
        when(jwtService.generateToken("a@b.com")).thenReturn("tok");

        assertEquals("tok", authService.login(new LoginRequest("a@b.com", "pw")).token());
    }
}
