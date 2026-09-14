package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.NotificationRequestDto;
import com.utown.utownbackend.dto.NotificationResponseDto;
import com.utown.utownbackend.dto.UnreadNotificationCountDto;
import com.utown.utownbackend.entity.NotificationType;
import com.utown.utownbackend.service.NotificationService;
import com.utown.utownbackend.util.TestDataFactory;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationService notificationService;

    private NotificationRequestDto requestDto;
    private NotificationResponseDto responseDto;

    @BeforeEach
    void setUp() {
        requestDto = TestDataFactory.createNotificationRequestDto(1L, NotificationType.ORDER_STATUS_CHANGED);
        responseDto = TestDataFactory.createNotificationResponseDto(10L, 1L);
    }

    @Test
    @DisplayName("POST /api/notifications - should return 201 Created")
    void createNotification_shouldReturn201() throws Exception {
        when(notificationService.createNotification(any(NotificationRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.title").value("Order Update"))
                .andExpect(jsonPath("$.type").value("ORDER_STATUS_CHANGED"));
    }

    @Test
    @DisplayName("POST /api/notifications - validation failure when title is blank")
    void createNotification_blankTitle_shouldReturn400() throws Exception {
        NotificationRequestDto invalid = new NotificationRequestDto(
                1L, NotificationType.ORDER_STATUS_CHANGED, "", "Valid message"
        );

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @DisplayName("GET /api/notifications?userId=1 - should return 200 OK with list")
    void getNotifications_queryParam_shouldReturn200() throws Exception {
        when(notificationService.getNotificationsForUser(1L, null)).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/notifications").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10L));
    }

    @Test
    @DisplayName("GET /api/notifications?userId=1&isRead=false - should return 200 OK with filtered list")
    void getNotifications_withIsReadFilter_shouldReturn200() throws Exception {
        when(notificationService.getNotificationsForUser(1L, false)).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/notifications").param("userId", "1").param("isRead", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(notificationService).getNotificationsForUser(1L, false);
    }

    @Test
    @DisplayName("GET /api/notifications - missing userId should return 400 Bad Request")
    void getNotifications_missingUserId_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("POST /api/notifications - missing userId should return 400 Bad Request")
    void createNotification_missingUserId_shouldReturn400() throws Exception {
        NotificationRequestDto invalid = new NotificationRequestDto(
                null, NotificationType.ORDER_STATUS_CHANGED, "Title", "Message"
        );

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @DisplayName("POST /api/notifications - missing type should return 400 Bad Request")
    void createNotification_missingType_shouldReturn400() throws Exception {
        NotificationRequestDto invalid = new NotificationRequestDto(
                1L, null, "Title", "Message"
        );

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @DisplayName("POST /api/notifications - blank message should return 400 Bad Request")
    void createNotification_blankMessage_shouldReturn400() throws Exception {
        NotificationRequestDto invalid = new NotificationRequestDto(
                1L, NotificationType.ORDER_STATUS_CHANGED, "Title", ""
        );

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @DisplayName("GET /api/notifications/{id}?userId=1 - should return 200 OK when found")
    void getNotificationById_shouldReturn200() throws Exception {
        when(notificationService.getNotificationById(10L, 1L)).thenReturn(responseDto);

        mockMvc.perform(get("/api/notifications/{id}", 10L).param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    @DisplayName("GET /api/notifications/{id}?userId=1 - should return 404 when not found")
    void getNotificationById_notFound_shouldReturn404() throws Exception {
        when(notificationService.getNotificationById(99L, 1L))
                .thenThrow(new EntityNotFoundException("Notification not found with id: 99"));

        mockMvc.perform(get("/api/notifications/{id}", 99L).param("userId", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("GET /api/notifications/unread-count?userId=1 - should return 200 OK with count")
    void getUnreadCount_shouldReturn200() throws Exception {
        when(notificationService.getUnreadCount(1L)).thenReturn(new UnreadNotificationCountDto(3L));

        mockMvc.perform(get("/api/notifications/unread-count").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3L));
    }

    @Test
    @DisplayName("PATCH /api/notifications/{id}/read?userId=1 - should return 200 OK")
    void markAsRead_shouldReturn200() throws Exception {
        when(notificationService.markAsRead(10L, 1L)).thenReturn(responseDto);

        mockMvc.perform(patch("/api/notifications/{id}/read", 10L).param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    @DisplayName("PATCH /api/notifications/read-all?userId=1 - should return 204 No Content")
    void markAllAsRead_shouldReturn204() throws Exception {
        doNothing().when(notificationService).markAllAsRead(1L);

        mockMvc.perform(patch("/api/notifications/read-all").param("userId", "1"))
                .andExpect(status().isNoContent());

        verify(notificationService).markAllAsRead(1L);
    }

    @Test
    @DisplayName("DELETE /api/notifications/{id}?userId=1 - should return 204 No Content")
    void deleteNotification_shouldReturn204() throws Exception {
        doNothing().when(notificationService).deleteNotification(10L, 1L);

        mockMvc.perform(delete("/api/notifications/{id}", 10L).param("userId", "1"))
                .andExpect(status().isNoContent());

        verify(notificationService).deleteNotification(10L, 1L);
    }
}
