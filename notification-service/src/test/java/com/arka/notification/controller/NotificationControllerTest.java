package com.arka.notification.controller;

import com.arka.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private NotificationService notificationService;

  @Test
  void sendNotification_ShouldReturnAccepted() throws Exception {
    Map<String, Object> payload = new HashMap<>();
    payload.put("type", "ORDER_CONFIRMED");
    payload.put("to", "user@example.com");
    payload.put("orderId", "12345");

    doNothing().when(notificationService).processNotification(any());

    mockMvc.perform(post("/api/notifications/send")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("queued"));

    verify(notificationService, times(1)).processNotification(any());
  }

  @Test
  void sendNotification_ShouldHandleEmptyPayload() throws Exception {
    Map<String, Object> emptyPayload = new HashMap<>();

    doNothing().when(notificationService).processNotification(any());

    mockMvc.perform(post("/api/notifications/send")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(emptyPayload)))
        .andExpect(status().isAccepted());
  }
}
