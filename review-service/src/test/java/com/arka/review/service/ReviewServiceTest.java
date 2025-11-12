package com.arka.review.service;

import com.arka.review.model.Review;
import com.arka.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

  @Mock
  private ReviewRepository reviewRepository;

  @InjectMocks
  private ReviewService reviewService;

  private Review testReview;

  @BeforeEach
  void setUp() {
    testReview = new Review();
    testReview.setId("123");
    testReview.setProductId(1L);
    testReview.setUserId(100L);
    testReview.setRating(5);
    testReview.setComment("Great product!");
  }

  @Test
  void create_ShouldCreateReview_WhenValidRating() {
    when(reviewRepository.save(any(Review.class))).thenReturn(testReview);

    Review result = reviewService.create(testReview);

    assertNotNull(result);
    assertEquals(5, result.getRating());
    verify(reviewRepository, times(1)).save(any(Review.class));
  }

  @Test
  void create_ShouldThrowException_WhenRatingTooLow() {
    testReview.setRating(0);

    assertThrows(IllegalArgumentException.class, () -> reviewService.create(testReview));
    verify(reviewRepository, never()).save(any(Review.class));
  }

  @Test
  void create_ShouldThrowException_WhenRatingTooHigh() {
    testReview.setRating(6);

    assertThrows(IllegalArgumentException.class, () -> reviewService.create(testReview));
    verify(reviewRepository, never()).save(any(Review.class));
  }

  @Test
  void create_ShouldThrowException_WhenRatingNull() {
    testReview.setRating(null);

    assertThrows(IllegalArgumentException.class, () -> reviewService.create(testReview));
    verify(reviewRepository, never()).save(any(Review.class));
  }

  @Test
  void findByProductId_ShouldReturnReviews() {
    when(reviewRepository.findByProductId(anyLong())).thenReturn(Arrays.asList(testReview));

    var result = reviewService.findByProductId(1L);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(1L, result.get(0).getProductId());
  }

  @Test
  void findByUserId_ShouldReturnReviews() {
    when(reviewRepository.findByUserId(anyLong())).thenReturn(Arrays.asList(testReview));

    var result = reviewService.findByUserId(100L);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(100L, result.get(0).getUserId());
  }
}
