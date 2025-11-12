package com.arka.cart.controller;

import com.arka.cart.model.Cart;
import com.arka.cart.service.AbandonedCartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/carts/abandoned")
public class AbandonedCartController {
  private static final Logger log = LoggerFactory.getLogger(AbandonedCartController.class);
  private final AbandonedCartService abandonedCartService;

  public AbandonedCartController(AbandonedCartService abandonedCartService) {
    this.abandonedCartService = abandonedCartService;
    log.info("AbandonedCartController initialized with base path: /api/carts/abandoned");
  }

  @GetMapping
  public ResponseEntity<List<Cart>> getAbandonedCarts(@RequestParam(defaultValue = "7") int daysThreshold) {
    log.info("🔍 GET /api/carts/abandoned - daysThreshold: {}", daysThreshold);
    List<Cart> abandonedCarts = abandonedCartService.findAbandonedCarts(daysThreshold);
    log.info("📦 Found {} abandoned carts", abandonedCarts.size());
    return ResponseEntity.ok(abandonedCarts);
  }

  @PostMapping("/{cartId}/remind")
  public ResponseEntity<Map<String, String>> sendReminder(@PathVariable Long cartId) {
    log.info("📧 POST /api/carts/abandoned/{}/remind", cartId);
    abandonedCartService.sendReminderEmail(cartId);
    return ResponseEntity.ok(Map.of("message", "Reminder sent successfully", "cartId", cartId.toString()));
  }

  @GetMapping("/stats")
  public ResponseEntity<Map<String, Object>> getAbandonedCartStats(@RequestParam(defaultValue = "7") int daysThreshold) {
    log.info("📊 GET /api/carts/abandoned/stats - daysThreshold: {}", daysThreshold);
    Map<String, Object> stats = abandonedCartService.getAbandonedCartStats(daysThreshold);
    return ResponseEntity.ok(stats);
  }
}