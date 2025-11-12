package com.arka.category.controller;

import com.arka.category.model.Category;
import com.arka.category.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
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
  void getAllCategories_ShouldReturnList() throws Exception {
    when(categoryService.findAll()).thenReturn(Arrays.asList(testCategory));

    mockMvc.perform(get("/api/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Electronics"));
  }

  @Test
  void getCategoryById_ShouldReturnCategory_WhenExists() throws Exception {
    when(categoryService.findById(anyLong())).thenReturn(Optional.of(testCategory));

    mockMvc.perform(get("/api/categories/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Electronics"));
  }

  @Test
  void createCategory_ShouldReturnCreated() throws Exception {
    when(categoryService.create(any(Category.class))).thenReturn(testCategory);

    mockMvc.perform(post("/api/categories")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testCategory)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Electronics"));
  }

  @Test
  void updateCategory_ShouldReturnUpdated() throws Exception {
    when(categoryService.update(anyLong(), any(Category.class))).thenReturn(testCategory);

    mockMvc.perform(put("/api/categories/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testCategory)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Electronics"));
  }

  @Test
  void deleteCategory_ShouldReturnNoContent() throws Exception {
    mockMvc.perform(delete("/api/categories/1"))
        .andExpect(status().isNoContent());
  }
}
