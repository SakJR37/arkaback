package com.arka.order.service;

import com.arka.order.clients.InventoryClient;
import com.arka.order.clients.NotificationClient;
import com.arka.order.model.Order;
import com.arka.order.model.OrderItem;
import com.arka.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock
  private OrderRepository orderRepository;

  @Mock
  private InventoryClient inventoryClient;

  @Mock
  private NotificationClient notificationClient;

  @InjectMocks
  private OrderService orderService;

  private Order testOrder;
  private OrderItem testItem;

  @BeforeEach
  void setUp() {
    testOrder = new Order();
    testOrder.setId(1L);
    // Removido setUserId() - no existe en la clase Order
    testOrder.setStatus("PENDING");
    testOrder.setCustomerEmail("test@example.com");
    
    testItem = new OrderItem();
    testItem.setProductId(1L);
    testItem.setQuantity(2);
    testItem.setPrice(49.99); // double, no BigDecimal
    
    List<OrderItem> items = new ArrayList<>();
    items.add(testItem);
    testOrder.setItems(items);
  }

  @Test
  void createOrder_ShouldCreateAndConfirm_WhenStockAvailable() {
    when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
    // inventoryClient.updateStock() retorna void, no boolean
    doNothing().when(inventoryClient).updateStock(anyLong(), anyInt(), anyString());

    Order result = orderService.createOrder(testOrder);

    assertNotNull(result);
    verify(orderRepository, atLeastOnce()).save(any(Order.class));
    verify(inventoryClient, times(1)).updateStock(anyLong(), anyInt(), anyString());
  }

  @Test
  void createOrder_ShouldCancel_WhenStockNotAvailable() {
    when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
    // Simular excepción cuando falla la reserva de inventario
    doThrow(new RuntimeException("Stock not available"))
        .when(inventoryClient).updateStock(anyLong(), anyInt(), anyString());

    assertThrows(RuntimeException.class, () -> orderService.createOrder(testOrder));

    verify(orderRepository, atLeastOnce()).save(any(Order.class));
  }

  @Test
  void modifyOrder_ShouldUpdateItems_WhenPending() {
    testOrder.setStatus("PENDING");
    when(orderRepository.findById(anyLong())).thenReturn(Optional.of(testOrder));
    when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
    doNothing().when(inventoryClient).updateStock(anyLong(), anyInt(), anyString());

    List<OrderItem> newItems = new ArrayList<>();
    OrderItem newItem = new OrderItem();
    newItem.setProductId(1L);
    newItem.setQuantity(5);
    newItem.setPrice(49.99); // double, no BigDecimal
    newItems.add(newItem);

    Order result = orderService.modifyOrder(1L, newItems);

    assertNotNull(result);
    verify(orderRepository, times(1)).save(any(Order.class));
  }

  @Test
  void modifyOrder_ShouldThrowException_WhenNotPending() {
    testOrder.setStatus("CONFIRMED");
    when(orderRepository.findById(anyLong())).thenReturn(Optional.of(testOrder));

    List<OrderItem> newItems = new ArrayList<>();

    assertThrows(IllegalStateException.class, () -> orderService.modifyOrder(1L, newItems));
  }

  @Test
  void modifyOrder_ShouldThrowException_WhenNotFound() {
    when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class, () -> orderService.modifyOrder(999L, new ArrayList<>()));
  }
}