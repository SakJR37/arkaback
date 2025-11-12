package com.arka.cart.controller;

import com.arka.cart.model.Cart;
import com.arka.cart.model.CartItem;
import com.arka.cart.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carts")
public class CartController {
  private final CartService cartService;

  public CartController(CartService cartService) {
    this.cartService = cartService;
  }

  
  @GetMapping("/{userId:\\d+}")
  public ResponseEntity<Cart> getCart(@PathVariable Long userId) {
    return cartService.findByUserId(userId)
      .map(ResponseEntity::ok)
      .orElse(ResponseEntity.ok(cartService.getOrCreateCart(userId)));
  }

  @PostMapping("/{userId:\\d+}/items")
  public ResponseEntity<Cart> addItem(@PathVariable Long userId, @RequestBody CartItem item) {
    Cart cart = cartService.addItem(userId, item);
    return ResponseEntity.ok(cart);
  }

  @DeleteMapping("/{userId:\\d+}/items/{productId:\\d+}")
  public ResponseEntity<Cart> removeItem(@PathVariable Long userId, @PathVariable Long productId) {
    Cart cart = cartService.removeItem(userId, productId);
    return ResponseEntity.ok(cart);
  }

  @PutMapping("/{userId:\\d+}/items/{productId:\\d+}")
  public ResponseEntity<Cart> updateQuantity(@PathVariable Long userId, 
                                              @PathVariable Long productId, 
                                              @RequestParam Integer quantity) {
    Cart cart = cartService.updateItemQuantity(userId, productId, quantity);
    return ResponseEntity.ok(cart);
  }

  @DeleteMapping("/{userId:\\d+}")
  public ResponseEntity<Void> clearCart(@PathVariable Long userId) {
    cartService.clearCart(userId);
    return ResponseEntity.noContent().build();
  }
}