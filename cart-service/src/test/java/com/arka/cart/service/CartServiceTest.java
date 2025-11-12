package com.arka.cart.service;

import com.arka.cart.model.Cart;
import com.arka.cart.model.CartItem;
import com.arka.cart.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

  @Mock
  private CartRepository cartRepository;

  @InjectMocks
  private CartService cartService;

  private Cart testCart;
  private CartItem testItem;

  @BeforeEach
  void setUp() {
    testCart = new Cart();
    testCart.setId(1L);
    testCart.setUserId(100L);
    testCart.setItems(new ArrayList<>());
    
    testItem = new CartItem();
    testItem.setProductId(1L);
    testItem.setQuantity(2);
    testItem.setPrice(49.99); // Cambiado de BigDecimal.valueOf(49.99) a 49.99
  }

  @Test
  void getOrCreateCart_ShouldReturnExistingCart_WhenExists() {
    when(cartRepository.findByUserId(anyLong())).thenReturn(Optional.of(testCart));

    Cart result = cartService.getOrCreateCart(100L);

    assertNotNull(result);
    assertEquals(100L, result.getUserId());
    verify(cartRepository, never()).save(any(Cart.class));
  }

  @Test
  void getOrCreateCart_ShouldCreateNewCart_WhenNotExists() {
    when(cartRepository.findByUserId(anyLong())).thenReturn(Optional.empty());
    when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

    Cart result = cartService.getOrCreateCart(100L);

    assertNotNull(result);
    verify(cartRepository, times(1)).save(any(Cart.class));
  }

  @Test
  void addItem_ShouldAddNewItem_WhenNotExists() {
    when(cartRepository.findByUserId(anyLong())).thenReturn(Optional.of(testCart));
    when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

    Cart result = cartService.addItem(100L, testItem);

    assertNotNull(result);
    assertEquals(1, result.getItems().size());
    verify(cartRepository, times(1)).save(any(Cart.class));
  }

  @Test
  void addItem_ShouldUpdateQuantity_WhenItemExists() {
    testCart.getItems().add(testItem);
    when(cartRepository.findByUserId(anyLong())).thenReturn(Optional.of(testCart));
    when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

    CartItem newItem = new CartItem();
    newItem.setProductId(1L);
    newItem.setQuantity(3);

    Cart result = cartService.addItem(100L, newItem);

    assertNotNull(result);
    assertEquals(1, result.getItems().size());
    assertEquals(5, result.getItems().get(0).getQuantity());
  }

  @Test
  void removeItem_ShouldRemoveItem() {
    testCart.getItems().add(testItem);
    when(cartRepository.findByUserId(anyLong())).thenReturn(Optional.of(testCart));
    when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

    Cart result = cartService.removeItem(100L, 1L);

    assertNotNull(result);
    assertEquals(0, result.getItems().size());
  }

  @Test
  void clearCart_ShouldClearAllItems() {
    testCart.getItems().add(testItem);
    when(cartRepository.findByUserId(anyLong())).thenReturn(Optional.of(testCart));

    cartService.clearCart(100L);

    verify(cartRepository, times(1)).save(any(Cart.class));
  }
}