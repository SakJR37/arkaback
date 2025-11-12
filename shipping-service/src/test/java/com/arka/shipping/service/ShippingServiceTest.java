package com.arka.shipping.service;

import com.arka.shipping.model.ShippingOption;
import com.arka.shipping.repository.ShippingOptionRepository;
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
class ShippingServiceTest {

  @Mock
  private ShippingOptionRepository shippingOptionRepository;

  @InjectMocks
  private ShippingService shippingService;

  private ShippingOption testOption;

  @BeforeEach
  void setUp() {
    testOption = new ShippingOption();
    testOption.setId(1L);
    testOption.setName("Express Delivery");
    testOption.setCost(BigDecimal.valueOf(19.99));
    testOption.setEstimatedDays(2);
    testOption.setActive(true);
  }

  @Test
  void findAll_ShouldReturnAllOptions() {
    when(shippingOptionRepository.findAll()).thenReturn(Arrays.asList(testOption));

    var result = shippingService.findAll();

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("Express Delivery", result.get(0).getName());
  }

  @Test
  void findActive_ShouldReturnOnlyActive() {
    when(shippingOptionRepository.findByActiveTrue()).thenReturn(Arrays.asList(testOption));

    var result = shippingService.findActive();

    assertNotNull(result);
    assertEquals(1, result.size());
    assertTrue(result.get(0).getActive());
  }

  @Test
  void findById_ShouldReturnOption_WhenExists() {
    when(shippingOptionRepository.findById(anyLong())).thenReturn(Optional.of(testOption));

    Optional<ShippingOption> result = shippingService.findById(1L);

    assertTrue(result.isPresent());
    assertEquals("Express Delivery", result.get().getName());
  }

  @Test
  void findById_ShouldReturnEmpty_WhenNotExists() {
    when(shippingOptionRepository.findById(anyLong())).thenReturn(Optional.empty());

    Optional<ShippingOption> result = shippingService.findById(999L);

    assertFalse(result.isPresent());
  }
}
