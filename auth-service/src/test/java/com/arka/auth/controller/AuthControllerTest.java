package com.arka.auth.controller;

import com.arka.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.cloud.config.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "eureka.client.enabled=false",
    "jwt.secret=testSecretKeyForJwtTokenGenerationAndValidationInTests123456789",
    "jwt.expiration=3600000"
})
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private AuthService authService;

  @Test
  void register_ShouldReturnCreated_WhenValidUser() throws Exception {
    Map<String, String> registerResponse = new HashMap<>();
    registerResponse.put("message", "User registered successfully");
    registerResponse.put("username", "testuser");
    
    when(authService.register("testuser", "test@example.com", "password123"))
        .thenReturn(registerResponse);

    Map<String, String> registerRequest = new HashMap<>();
    registerRequest.put("username", "testuser");
    registerRequest.put("email", "test@example.com");
    registerRequest.put("password", "password123");

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(registerRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.message").value("User registered successfully"))
        .andExpect(jsonPath("$.username").value("testuser"));
  }

  @Test
  void login_ShouldReturnToken_WhenValidCredentials() throws Exception {
    Map<String, String> tokenResponse = new HashMap<>();
    tokenResponse.put("token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");
    tokenResponse.put("message", "Login successful");
    tokenResponse.put("username", "testuser");

    when(authService.login("testuser", "password123")).thenReturn(tokenResponse);

    Map<String, String> loginRequest = new HashMap<>();
    loginRequest.put("username", "testuser");
    loginRequest.put("password", "password123");

    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."))
        .andExpect(jsonPath("$.message").value("Login successful"))
        .andExpect(jsonPath("$.username").value("testuser"));
  }

  @Test
  void register_ShouldReturnBadRequest_WhenUserAlreadyExists() throws Exception {
    when(authService.register("testuser", "test@example.com", "password123"))
        .thenThrow(new RuntimeException("Username already exists"));

    Map<String, String> registerRequest = new HashMap<>();
    registerRequest.put("username", "testuser");
    registerRequest.put("email", "test@example.com");
    registerRequest.put("password", "password123");

    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(registerRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Username already exists"));
  }

  @Test
  void login_ShouldReturnUnauthorized_WhenInvalidCredentials() throws Exception {
    when(authService.login("testuser", "wrongpassword"))
        .thenThrow(new RuntimeException("Invalid credentials"));

    Map<String, String> loginRequest = new HashMap<>();
    loginRequest.put("username", "testuser");
    loginRequest.put("password", "wrongpassword");

    mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Invalid credentials"));
  }

  @Test
  void validateToken_ShouldReturnTrue_WhenTokenIsValid() throws Exception {
    when(authService.validateToken(anyString())).thenReturn(true);

    mockMvc.perform(post("/auth/validate")
            .header("Authorization", "Bearer valid.jwt.token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valid").value(true));
  }

  @Test
  void validateToken_ShouldReturnFalse_WhenTokenIsInvalid() throws Exception {
    when(authService.validateToken(anyString())).thenReturn(false);

    mockMvc.perform(post("/auth/validate")
            .header("Authorization", "Bearer invalid.jwt.token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valid").value(false));
  }
}