package com.arka.auth.service;

import com.arka.auth.model.User;
import com.arka.auth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private JwtService jwtService;

  @Spy
  private BCryptPasswordEncoder passwordEncoder;

  @InjectMocks
  private AuthService authService;

  private User testUser;
  private String rawPassword = "password123";
  private String encodedPassword;

  @BeforeEach
  void setUp() {
    // Generar password encriptado real para usar en los tests
    encodedPassword = new BCryptPasswordEncoder().encode(rawPassword);
    
    testUser = new User();
    testUser.setId(1L);
    testUser.setUsername("testuser");
    testUser.setEmail("test@example.com");
    testUser.setPassword(encodedPassword);
  }

  @Test
  void register_ShouldCreateUser_WhenValidData() {
    when(userRepository.existsByUsername("newuser")).thenReturn(false);
    when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
    when(userRepository.save(any(User.class))).thenReturn(testUser);

    Map<String, String> result = authService.register("newuser", "new@example.com", rawPassword);

    assertNotNull(result);
    assertEquals("User registered successfully", result.get("message"));
    assertEquals("newuser", result.get("username"));
    verify(userRepository, times(1)).existsByUsername("newuser");
    verify(userRepository, times(1)).existsByEmail("new@example.com");
    verify(userRepository, times(1)).save(any(User.class));
  }

  @Test
  void register_ShouldThrowException_WhenUsernameExists() {
    when(userRepository.existsByUsername("existinguser")).thenReturn(true);

    RuntimeException exception = assertThrows(RuntimeException.class, 
        () -> authService.register("existinguser", "existing@example.com", rawPassword));
    
    assertEquals("Username already exists", exception.getMessage());
    verify(userRepository, times(1)).existsByUsername("existinguser");
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void register_ShouldThrowException_WhenEmailExists() {
    when(userRepository.existsByUsername("newuser")).thenReturn(false);
    when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

    RuntimeException exception = assertThrows(RuntimeException.class, 
        () -> authService.register("newuser", "existing@example.com", rawPassword));
    
    assertEquals("Email already exists", exception.getMessage());
    verify(userRepository, times(1)).existsByUsername("newuser");
    verify(userRepository, times(1)).existsByEmail("existing@example.com");
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void login_ShouldReturnToken_WhenCredentialsValid() {
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    when(jwtService.generateToken(eq("testuser"), anyMap())).thenReturn("jwt-token-123");

    Map<String, String> result = authService.login("testuser", rawPassword);

    assertNotNull(result);
    assertEquals("jwt-token-123", result.get("token"));
    assertEquals("Login successful", result.get("message"));
    assertEquals("testuser", result.get("username"));
    verify(userRepository, times(1)).findByUsername("testuser");
    verify(jwtService, times(1)).generateToken(eq("testuser"), anyMap());
  }

  @Test
  void login_ShouldThrowException_WhenUserNotFound() {
    when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

    RuntimeException exception = assertThrows(RuntimeException.class, 
        () -> authService.login("nonexistent", rawPassword));
    
    assertEquals("Invalid credentials", exception.getMessage());
    verify(userRepository, times(1)).findByUsername("nonexistent");
    verify(jwtService, never()).generateToken(anyString(), anyMap());
  }

  @Test
  void login_ShouldThrowException_WhenInvalidPassword() {
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

    RuntimeException exception = assertThrows(RuntimeException.class, 
        () -> authService.login("testuser", "wrongpassword"));
    
    assertEquals("Invalid credentials", exception.getMessage());
    verify(userRepository, times(1)).findByUsername("testuser");
    verify(jwtService, never()).generateToken(anyString(), anyMap());
  }

  @Test
  void validateToken_ShouldReturnTrue_WhenTokenIsValid() {
    String validToken = "valid.jwt.token";
    
    // validateToken() retorna Claims, mock con when().thenReturn()
    Claims mockClaims = mock(Claims.class);
    when(jwtService.validateToken(validToken)).thenReturn(mockClaims);
    when(jwtService.isTokenExpired(validToken)).thenReturn(false);

    boolean result = authService.validateToken(validToken);

    assertTrue(result);
    verify(jwtService, times(1)).validateToken(validToken);
    verify(jwtService, times(1)).isTokenExpired(validToken);
  }

  @Test
  void validateToken_ShouldReturnFalse_WhenTokenIsExpired() {
    String expiredToken = "expired.jwt.token";
    
    Claims mockClaims = mock(Claims.class);
    when(jwtService.validateToken(expiredToken)).thenReturn(mockClaims);
    when(jwtService.isTokenExpired(expiredToken)).thenReturn(true);

    boolean result = authService.validateToken(expiredToken);

    assertFalse(result);
    verify(jwtService, times(1)).validateToken(expiredToken);
    verify(jwtService, times(1)).isTokenExpired(expiredToken);
  }

  @Test
  void validateToken_ShouldReturnFalse_WhenTokenIsInvalid() {
    String invalidToken = "invalid.jwt.token";
    
    doThrow(new RuntimeException("Invalid token")).when(jwtService).validateToken(invalidToken);

    boolean result = authService.validateToken(invalidToken);

    assertFalse(result);
    verify(jwtService, times(1)).validateToken(invalidToken);
    verify(jwtService, never()).isTokenExpired(anyString());
  }
}