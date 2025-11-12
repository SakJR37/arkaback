package com.arka.catalog.service;

import com.arka.catalog.model.ProductCatalog;
import com.arka.catalog.repository.ProductCatalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

  @Mock
  private ProductCatalogRepository catalogRepository;

  @InjectMocks
  private CatalogService catalogService;

  private ProductCatalog testProduct;

  @BeforeEach
  void setUp() {
    testProduct = new ProductCatalog();
    testProduct.setId(1L);
    testProduct.setProductId(100L);
    testProduct.setName("Test Product");
    testProduct.setPrice(BigDecimal.valueOf(99.99));
    testProduct.setStock(50);
    testProduct.setCategory("Electronics");
    testProduct.setActive(true);
  }

  @Test
  void createOrUpdate_ShouldCreateNew_WhenNotExists() {
    when(catalogRepository.findByProductId(anyLong())).thenReturn(Optional.empty());
    when(catalogRepository.save(any(ProductCatalog.class))).thenReturn(testProduct);

    ProductCatalog result = catalogService.createOrUpdate(testProduct);

    assertNotNull(result);
    assertEquals("Test Product", result.getName());
    verify(catalogRepository, times(1)).save(any(ProductCatalog.class));
  }

  @Test
  void createOrUpdate_ShouldUpdate_WhenExists() {
    when(catalogRepository.findByProductId(anyLong())).thenReturn(Optional.of(testProduct));
    when(catalogRepository.save(any(ProductCatalog.class))).thenReturn(testProduct);

    ProductCatalog updated = new ProductCatalog();
    updated.setProductId(100L);
    updated.setName("Updated Product");

    ProductCatalog result = catalogService.createOrUpdate(updated);

    assertNotNull(result);
    verify(catalogRepository, times(1)).save(any(ProductCatalog.class));
  }

  @Test
  void findAll_ShouldReturnAllProducts() {
    when(catalogRepository.findAll()).thenReturn(Arrays.asList(testProduct));

    List<ProductCatalog> result = catalogService.findAll();

    assertNotNull(result);
    assertEquals(1, result.size());
  }

  @Test
  void findActive_ShouldReturnOnlyActive() {
    when(catalogRepository.findByActiveTrue()).thenReturn(Arrays.asList(testProduct));

    List<ProductCatalog> result = catalogService.findActive();

    assertNotNull(result);
    assertEquals(1, result.size());
    assertTrue(result.get(0).getActive());
  }

  @Test
  void deactivate_ShouldSetActiveFalse() {
    when(catalogRepository.findById(anyLong())).thenReturn(Optional.of(testProduct));

    catalogService.deactivate(1L);

    verify(catalogRepository, times(1)).save(any(ProductCatalog.class));
  }
}
