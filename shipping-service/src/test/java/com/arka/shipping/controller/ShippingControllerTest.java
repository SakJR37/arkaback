package com.arka.shipping.controller;

import com.arka.shipping.model.ShippingOption;
import com.arka.shipping.service.ShippingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShippingController.class)
class ShippingControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ShippingService shippingService;

  private ShippingOption testOption;

  @BeforeEach
  void setUp() {
    testOption = new ShippingOption();
    testOption.setId(1L);
    testOption.setName("Standard Delivery");
    testOption.setCost(BigDecimal.valueOf(9.99));
    testOption.setEstimatedDays(5);
    testOption.setActive(true);
  }

  @Test
  void getOptions_ShouldReturnAll_WhenActiveNotSpecified() throws Exception {
    when(shippingService.findAll()).thenReturn(Arrays.asList(testOption));

    mockMvc.perform(get("/api/shipping/options"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Standard Delivery"));
  }

  @Test
  void getOptions_ShouldReturnActive_WhenActiveTrue() throws Exception {
    when(shippingService.findActive()).thenReturn(Arrays.asList(testOption));

    mockMvc.perform(get("/api/shipping/options?active=true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].active").value(true));
  }

  @Test
  void getOption_ShouldReturnOption_WhenExists() throws Exception {
    when(shippingService.findById(anyLong())).thenReturn(Optional.of(testOption));

    mockMvc.perform(get("/api/shipping/options/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Standard Delivery"));
  }

  @Test
  void getOption_ShouldReturnNotFound_WhenNotExists() throws Exception {
    when(shippingService.findById(anyLong())).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/shipping/options/999"))
        .andExpect(status().isNotFound());
  }
}
