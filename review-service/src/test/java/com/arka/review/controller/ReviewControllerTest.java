package com.arka.review.controller;

import com.arka.review.model.Review;
import com.arka.review.service.ReviewService;
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

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ReviewService reviewService;

  private Review testReview;

  @BeforeEach
  void setUp() {
    testReview = new Review();
    testReview.setId("123");
    testReview.setProductId(1L);
    testReview.setUserId(100L);
    testReview.setRating(5);
    testReview.setComment("Excellent product!");
  }

  @Test
  void getReviewsByProduct_ShouldReturnList() throws Exception {
    when(reviewService.findByProductId(anyLong())).thenReturn(Arrays.asList(testReview));

    mockMvc.perform(get("/api/reviews/product/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].comment").value("Excellent product!"));
  }

  @Test
  void getReviewsByUser_ShouldReturnList() throws Exception {
    when(reviewService.findByUserId(anyLong())).thenReturn(Arrays.asList(testReview));

    mockMvc.perform(get("/api/reviews/user/100"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].rating").value(5));
  }

  @Test
  void createReview_ShouldReturnCreated() throws Exception {
    when(reviewService.create(any(Review.class))).thenReturn(testReview);

    mockMvc.perform(post("/api/reviews")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testReview)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.rating").value(5));
  }

  @Test
  void deleteReview_ShouldReturnNoContent() throws Exception {
    mockMvc.perform(delete("/api/reviews/123"))
        .andExpect(status().isNoContent());
  }
}
