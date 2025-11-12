package com.arka.inventory.service;

import com.arka.inventory.model.Product;
import com.arka.inventory.repository.ProductRepository;
import com.arka.inventory.repository.StockHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  @Mock
  private ProductRepository productRepository;

  @Mock
  private StockHistoryRepository historyRepository;

  @InjectMocks
  private ProductService productService;

  private Product testProduct;

  @BeforeEach
  void setUp() {
    testProduct = new Product();
    testProduct.setId(1L);
    testProduct.setName("Test Product");
    testProduct.setPrice(BigDecimal.valueOf(99.99));
    testProduct.setStock(100);
  }

  @Test
  void create_ShouldCreateProduct_WhenValidData() {
    when(productRepository.save(any(Product.class))).thenReturn(testProduct);

    Product result = productService.create(testProduct);

    assertNotNull(result);
    assertEquals("Test Product", result.getName());
    verify(productRepository, times(1)).save(any(Product.class));
  }

  @Test
  void create_ShouldThrowException_WhenNegativePrice() {
    testProduct.setPrice(BigDecimal.valueOf(-10));

    assertThrows(IllegalArgumentException.class, () -> productService.create(testProduct));
    verify(productRepository, never()).save(any(Product.class));
  }

  @Test
  void create_ShouldThrowException_WhenNegativeStock() {
    testProduct.setStock(-5);

    assertThrows(IllegalArgumentException.class, () -> productService.create(testProduct));
    verify(productRepository, never()).save(any(Product.class));
  }

  @Test
  void updateStock_ShouldIncreaseStock_WhenPositiveDelta() {
    when(productRepository.findById(anyLong())).thenReturn(Optional.of(testProduct));
    when(productRepository.save(any(Product.class))).thenReturn(testProduct);

    Product result = productService.updateStock(1L, 50, "Restock");

    assertNotNull(result);
    verify(productRepository, times(1)).save(any(Product.class));
    verify(historyRepository, times(1)).save(any());
  }

  @Test
  void updateStock_ShouldDecreaseStock_WhenNegativeDelta() {
    when(productRepository.findById(anyLong())).thenReturn(Optional.of(testProduct));
    when(productRepository.save(any(Product.class))).thenReturn(testProduct);

    Product result = productService.updateStock(1L, -10, "Order");

    assertNotNull(result);
    verify(productRepository, times(1)).save(any(Product.class));
  }

  @Test
  void updateStock_ShouldThrowException_WhenStockWouldBeNegative() {
    when(productRepository.findById(anyLong())).thenReturn(Optional.of(testProduct));

    assertThrows(IllegalArgumentException.class, () -> productService.updateStock(1L, -200, "Order"));
  }

  @Test
  void findById_ShouldReturnProduct_WhenExists() {
    when(productRepository.findById(anyLong())).thenReturn(Optional.of(testProduct));

    Optional<Product> result = productService.findById(1L);

    assertTrue(result.isPresent());
    assertEquals("Test Product", result.get().getName());
  }

  @Test
  void lowStock_ShouldReturnLowStockProducts() {
    testProduct.setStock(5);
    when(productRepository.findByStockLessThan(anyInt())).thenReturn(Arrays.asList(testProduct));

    var result = productService.lowStock(10);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertTrue(result.get(0).getStock() < 10);
  }
}
