package com.arka.provider.service;

import com.arka.provider.model.Provider;
import com.arka.provider.repository.ProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderServiceTest {

  @Mock
  private ProviderRepository providerRepository;

  @InjectMocks
  private ProviderService providerService;

  private Provider testProvider;

  @BeforeEach
  void setUp() {
    testProvider = new Provider();
    testProvider.setId(1L);
    testProvider.setName("Global Suppliers");
    testProvider.setEmail("info@globalsuppliers.com");
    testProvider.setPhone("+9876543210");
    testProvider.setActive(true);
  }

  @Test
  void create_ShouldCreateProvider() {
    when(providerRepository.save(any(Provider.class))).thenReturn(testProvider);

    Provider result = providerService.create(testProvider);

    assertNotNull(result);
    assertEquals("Global Suppliers", result.getName());
    verify(providerRepository, times(1)).save(any(Provider.class));
  }

  @Test
  void update_ShouldUpdateProvider_WhenExists() {
    when(providerRepository.findById(anyLong())).thenReturn(Optional.of(testProvider));
    when(providerRepository.save(any(Provider.class))).thenReturn(testProvider);

    Provider updated = new Provider();
    updated.setName("Updated Supplier");
    updated.setEmail("new@email.com");

    Provider result = providerService.update(1L, updated);

    assertNotNull(result);
    verify(providerRepository, times(1)).save(any(Provider.class));
  }

  @Test
  void update_ShouldThrowException_WhenNotExists() {
    when(providerRepository.findById(anyLong())).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class, () -> providerService.update(1L, testProvider));
  }

  @Test
  void findAll_ShouldReturnAllProviders() {
    when(providerRepository.findAll()).thenReturn(Arrays.asList(testProvider));

    var result = providerService.findAll();

    assertNotNull(result);
    assertEquals(1, result.size());
  }

  @Test
  void findActive_ShouldReturnOnlyActive() {
    when(providerRepository.findByActiveTrue()).thenReturn(Arrays.asList(testProvider));

    var result = providerService.findActive();

    assertNotNull(result);
    assertEquals(1, result.size());
    assertTrue(result.get(0).getActive());
  }

  @Test
  void findById_ShouldReturnProvider_WhenExists() {
    when(providerRepository.findById(anyLong())).thenReturn(Optional.of(testProvider));

    Optional<Provider> result = providerService.findById(1L);

    assertTrue(result.isPresent());
    assertEquals("Global Suppliers", result.get().getName());
  }
}
