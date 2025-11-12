package com.arka.order.controller;

import com.arka.order.model.Order;
import com.arka.order.model.OrderItem;
import com.arka.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private OrderService orderService;

  private Order testOrder;

  @BeforeEach
  void setUp() {
    testOrder = new Order();
    testOrder.setId(1L);
    // Removido setUserId() - no existe en la clase Order
    testOrder.setStatus("PENDING");
    testOrder.setTotal(99.99); // Usa setTotal() con double, no setTotalAmount()
    testOrder.setCustomerEmail("test@example.com");
    
    List<OrderItem> items = new ArrayList<>();
    OrderItem item = new OrderItem();
    item.setProductId(1L);
    item.setQuantity(2);
    item.setPrice(49.99); // double, no BigDecimal
    items.add(item);
    testOrder.setItems(items);
  }

  @Test
  void createOrder_ShouldReturnCreated_WhenValidOrder() throws Exception {
    when(orderService.createOrder(any(Order.class))).thenReturn(testOrder);

    mockMvc.perform(post("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testOrder)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.total").value(99.99));
  }

  @Test
  void modifyOrder_ShouldReturnOk_WhenValidModification() throws Exception {
    List<OrderItem> newItems = new ArrayList<>();
    OrderItem newItem = new OrderItem();
    newItem.setProductId(2L);
    newItem.setQuantity(1);
    newItem.setPrice(29.99); // double, no BigDecimal
    newItems.add(newItem);

    testOrder.setItems(newItems);
    testOrder.setTotal(29.99);
    when(orderService.modifyOrder(anyLong(), any())).thenReturn(testOrder);

    mockMvc.perform(patch("/api/orders/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(newItems)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }
}