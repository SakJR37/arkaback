package com.arka.cart.controller;

import com.arka.cart.model.Cart;
import com.arka.cart.model.CartItem;
import com.arka.cart.service.CartService;
import com.arka.cart.service.AbandonedCartService;
import com.arka.cart.repository.CartRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "eureka.client.enabled=false",
    "springdoc.api-docs.enabled=false",
    "springdoc.swagger-ui.enabled=false"
})
class CartControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private CartService cartService;

  
  @MockBean
  private CartRepository cartRepository;
  
  @MockBean
  private AbandonedCartService abandonedCartService;

  private Cart testCart;
  private CartItem testItem;

  @BeforeEach
  void setUp() {
    testCart = new Cart();
    testCart.setId(1L);
    testCart.setUserId(100L);
    
    testItem = new CartItem();
    testItem.setProductId(1L);
    testItem.setQuantity(2);
    testItem.setPrice(49.99);
    
    List<CartItem> items = new ArrayList<>();
    items.add(testItem);
    testCart.setItems(items);
  }

  @Test
  void getCart_ShouldReturnCart_WhenExists() throws Exception {
    when(cartService.findByUserId(anyLong())).thenReturn(Optional.of(testCart));

    mockMvc.perform(get("/api/carts/100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.userId").value(100));
  }

  @Test
  void addItem_ShouldReturnUpdatedCart() throws Exception {
    when(cartService.addItem(anyLong(), any(CartItem.class))).thenReturn(testCart);

    mockMvc.perform(post("/api/carts/100/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testItem)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(100));
  }

  @Test
  void removeItem_ShouldReturnUpdatedCart() throws Exception {
    when(cartService.removeItem(anyLong(), anyLong())).thenReturn(testCart);

    mockMvc.perform(delete("/api/carts/100/items/1"))
        .andExpect(status().isOk());
  }

  @Test
  void clearCart_ShouldReturnNoContent() throws Exception {
    doNothing().when(cartService).clearCart(anyLong());

    mockMvc.perform(delete("/api/carts/100"))
        .andExpect(status().isNoContent());
  }
}