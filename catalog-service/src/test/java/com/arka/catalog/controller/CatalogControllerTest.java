package com.arka.catalog.controller;

import com.arka.catalog.model.ProductCatalog;
import com.arka.catalog.service.CatalogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CatalogController.class)
class CatalogControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private CatalogService catalogService;

  private ProductCatalog testProduct;

  @BeforeEach
  void setUp() {
    testProduct = new ProductCatalog();
    testProduct.setId(1L);
    testProduct.setProductId(100L);
    testProduct.setName("Test Product");
    testProduct.setDescription("Test Description");
    testProduct.setPrice(BigDecimal.valueOf(99.99));
    testProduct.setStock(50);
    testProduct.setCategory("Electronics");
    testProduct.setActive(true);
  }

  @Test
  void getAllProducts_ShouldReturnList() throws Exception {
    when(catalogService.findAll()).thenReturn(Arrays.asList(testProduct));

    mockMvc.perform(get("/api/catalog"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Test Product"));
  }

  @Test
  void getActiveProducts_ShouldReturnActiveOnly() throws Exception {
    when(catalogService.findActive()).thenReturn(Arrays.asList(testProduct));

    mockMvc.perform(get("/api/catalog?active=true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].active").value(true));
  }

  @Test
  void getProductById_ShouldReturnProduct_WhenExists() throws Exception {
    when(catalogService.findById(anyLong())).thenReturn(Optional.of(testProduct));

    mockMvc.perform(get("/api/catalog/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Test Product"));
  }

  @Test
  void createOrUpdateProduct_ShouldReturnProduct() throws Exception {
    when(catalogService.createOrUpdate(any(ProductCatalog.class))).thenReturn(testProduct);

    mockMvc.perform(post("/api/catalog")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testProduct)))
        .andExpect(status().isCreated())  
        .andExpect(jsonPath("$.name").value("Test Product"));
  }
}