package com.arka.category.service;

import com.arka.category.model.Category;
import com.arka.category.repository.CategoryRepository;
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
class CategoryServiceTest {

  @Mock
  private CategoryRepository categoryRepository;

  @InjectMocks
  private CategoryService categoryService;

  private Category testCategory;

  @BeforeEach
  void setUp() {
    testCategory = new Category();
    testCategory.setId(1L);
    testCategory.setName("Electronics");
    testCategory.setDescription("Electronic products");
    testCategory.setActive(true);
  }

  @Test
  void create_ShouldCreateCategory_WhenNameNotExists() {
    when(categoryRepository.findByName(anyString())).thenReturn(Optional.empty());
    when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

    Category result = categoryService.create(testCategory);

    assertNotNull(result);
    assertEquals("Electronics", result.getName());
    verify(categoryRepository, times(1)).save(any(Category.class));
  }

  @Test
  void create_ShouldThrowException_WhenNameExists() {
    when(categoryRepository.findByName(anyString())).thenReturn(Optional.of(testCategory));

    assertThrows(IllegalArgumentException.class, () -> categoryService.create(testCategory));
    verify(categoryRepository, never()).save(any(Category.class));
  }

  @Test
  void update_ShouldUpdateCategory_WhenExists() {
    when(categoryRepository.findById(anyLong())).thenReturn(Optional.of(testCategory));
    when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

    Category updated = new Category();
    updated.setName("Updated Electronics");

    Category result = categoryService.update(1L, updated);

    assertNotNull(result);
    verify(categoryRepository, times(1)).save(any(Category.class));
  }

  @Test
  void update_ShouldThrowException_WhenNotExists() {
    when(categoryRepository.findById(anyLong())).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class, () -> categoryService.update(1L, testCategory));
  }

  @Test
  void findAll_ShouldReturnAllCategories() {
    when(categoryRepository.findAll()).thenReturn(Arrays.asList(testCategory));

    var result = categoryService.findAll();

    assertNotNull(result);
    assertEquals(1, result.size());
  }

  @Test
  void findActive_ShouldReturnOnlyActive() {
    when(categoryRepository.findByActiveTrue()).thenReturn(Arrays.asList(testCategory));

    var result = categoryService.findActive();

    assertNotNull(result);
    assertEquals(1, result.size());
    assertTrue(result.get(0).getActive());
  }
}
