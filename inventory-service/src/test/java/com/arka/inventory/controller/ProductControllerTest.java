package com.arka.inventory.controller;

import com.arka.inventory.dto.ProductRequest;
import com.arka.inventory.model.Product;
import com.arka.inventory.service.ProductService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ProductService productService;

  private Product testProduct;

  @BeforeEach
  void setUp() {
    testProduct = new Product();
    testProduct.setId(1L);
    testProduct.setName("Test Product");
    testProduct.setCategory("Electronics");
    testProduct.setPrice(BigDecimal.valueOf(99.99));
    testProduct.setStock(100);
  }

  @Test
  void getAllProducts_ShouldReturnList() throws Exception {
    when(productService.listAll()).thenReturn(Arrays.asList(testProduct));

    mockMvc.perform(get("/api/products"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Test Product"));
  }

  @Test
  void getProductById_ShouldReturnProduct_WhenExists() throws Exception {
    when(productService.findById(anyLong())).thenReturn(Optional.of(testProduct));

    mockMvc.perform(get("/api/products/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Test Product"));
  }

  @Test
  void createProduct_ShouldReturnCreated() throws Exception {
    when(productService.create(any(Product.class))).thenReturn(testProduct);

    ProductRequest request = new ProductRequest();
    request.setName("Test Product");
    request.setCategory("Electronics");
    request.setPrice(BigDecimal.valueOf(99.99));
    request.setStock(100);

    mockMvc.perform(post("/api/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Test Product"));
  }

  @Test
  void updateStock_ShouldReturnUpdatedProduct() throws Exception {
    testProduct.setStock(110);
    when(productService.updateStock(anyLong(), anyInt(), anyString())).thenReturn(testProduct);

    // El endpoint usa @RequestParam, no un body JSON
    mockMvc.perform(put("/api/products/1/stock?delta=10&reason=Restock"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Test Product"))
        .andExpect(jsonPath("$.stock").value(110));
  }

  @Test
  void updateStock_ShouldReturnBadRequest_WhenInvalidStock() throws Exception {
    when(productService.updateStock(anyLong(), anyInt(), anyString()))
        .thenThrow(new IllegalArgumentException("Insufficient stock"));

    mockMvc.perform(put("/api/products/1/stock?delta=-200&reason=Sale"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getLowStock_ShouldReturnLowStockProducts() throws Exception {
    testProduct.setStock(5);
    when(productService.lowStock(anyInt())).thenReturn(Arrays.asList(testProduct));

    mockMvc.perform(get("/api/products/low-stock?threshold=10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Test Product"))
        .andExpect(jsonPath("$[0].stock").value(5));
  }

  @Test
  void getLowStock_ShouldUseDefaultThreshold() throws Exception {
    when(productService.lowStock(10)).thenReturn(Arrays.asList(testProduct));

    mockMvc.perform(get("/api/products/low-stock"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Test Product"));
  }
}