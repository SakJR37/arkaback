package com.arka.provider.controller;

import com.arka.provider.model.Provider;
import com.arka.provider.service.ProviderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProviderController.class)
class ProviderControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ProviderService providerService;

  private Provider testProvider;

  @BeforeEach
  void setUp() {
    testProvider = new Provider();
    testProvider.setId(1L);
    testProvider.setName("Tech Supplies Inc");
    testProvider.setEmail("contact@techsupplies.com");
    testProvider.setPhone("+1234567890");
    testProvider.setAddress("123 Tech Street");
    testProvider.setActive(true);
  }

  @Test
  void getAllProviders_ShouldReturnList() throws Exception {
    when(providerService.findAll()).thenReturn(Arrays.asList(testProvider));

    mockMvc.perform(get("/api/providers"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Tech Supplies Inc"));
  }

  @Test
  void getActiveProviders_ShouldReturnActiveOnly() throws Exception {
    when(providerService.findActive()).thenReturn(Arrays.asList(testProvider));

    mockMvc.perform(get("/api/providers?active=true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].active").value(true));
  }

  @Test
  void getProviderById_ShouldReturnProvider_WhenExists() throws Exception {
    when(providerService.findById(anyLong())).thenReturn(Optional.of(testProvider));

    mockMvc.perform(get("/api/providers/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Tech Supplies Inc"));
  }

  @Test
  void createProvider_ShouldReturnCreated() throws Exception {
    when(providerService.create(any(Provider.class))).thenReturn(testProvider);

    mockMvc.perform(post("/api/providers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testProvider)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Tech Supplies Inc"));
  }

  @Test
  void updateProvider_ShouldReturnUpdated() throws Exception {
    when(providerService.update(anyLong(), any(Provider.class))).thenReturn(testProvider);

    mockMvc.perform(put("/api/providers/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testProvider)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Tech Supplies Inc"));
  }
}
