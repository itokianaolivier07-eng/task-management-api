package com.taskflow.task_management_api.service;

import com.taskflow.task_management_api.dto.AuthResponse;
import com.taskflow.task_management_api.dto.LoginRequest;
import com.taskflow.task_management_api.dto.RegisterRequest;
import com.taskflow.task_management_api.entity.User;
import com.taskflow.task_management_api.repository.UserRepository;
import com.taskflow.task_management_api.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;
    @InjectMocks AuthService authService;

    @Test
    void registerRejectsExistingEmail() {
        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register(new RegisterRequest("Alice", "a@b.com", "password123")));

        assertEquals(409, ex.getStatusCode().value());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerHashesPasswordAndReturnsToken() {
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(jwtService.generateToken("a@b.com")).thenReturn("tok");

        AuthResponse response = authService.register(new RegisterRequest("Alice", "a@b.com", "password123"));

        assertEquals("tok", response.token());
        verify(userRepository).save(argThat((User u) -> "hashed".equals(u.getPassword())));
    }

    @Test
    void loginWithBadCredentialsFails() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequest("a@b.com", "wrong")));
    }
}
