package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.NotificationRequestDto;
import com.utown.utownbackend.dto.NotificationResponseDto;
import com.utown.utownbackend.dto.UnreadNotificationCountDto;
import com.utown.utownbackend.entity.NotificationType;
import com.utown.utownbackend.entity.UserRole;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import com.utown.utownbackend.security.CustomUserDetails;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(NotificationController.class)
@Import(NotificationControllerTest.MethodSecurityTestConfig.class)
@WithMockUser(roles = "CUSTOMER")
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

    private void setAuthenticatedUser(Long userId, UserRole role) {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        when(userDetails.getId()).thenReturn(userId);
        when(userDetails.getRole()).thenReturn(role);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
                );

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/notifications - admin can create notification")
    void createNotification_shouldReturn201() throws Exception {

        when(notificationService.createNotification(
                any(NotificationRequestDto.class)
        )).thenReturn(responseDto);

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
    @DisplayName("POST /api/notifications - customer cannot create notification")
    void createNotification_customer_shouldReturn403() throws Exception {

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(notificationService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
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
    @DisplayName("GET /api/notifications?userId=1 - customer can access own notifications")
    void getNotifications_queryParam_shouldReturn200() throws Exception {
        setAuthenticatedUser(1L, UserRole.CUSTOMER);
        when(notificationService.getNotificationsForUser(1L, null)).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/notifications").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10L));
    }

    @Test
    @DisplayName("GET /api/notifications?userId=2 - customer cannot access another user's notifications")
    void getNotifications_otherUser_shouldReturn403() throws Exception {

        setAuthenticatedUser(1L, UserRole.CUSTOMER);

        mockMvc.perform(
                        get("/api/notifications")
                                .param("userId", "2")
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("GET /api/notifications?userId=2 - restaurant owner cannot access another user's notifications")
    void getNotifications_restaurantOwner_otherUser_shouldReturn403() throws Exception {

        setAuthenticatedUser(1L, UserRole.RESTAURANT_OWNER);

        mockMvc.perform(
                        get("/api/notifications")
                                .param("userId", "2")
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("GET /api/notifications?userId=2 - rider cannot access another user's notifications")
    void getNotifications_rider_otherUser_shouldReturn403() throws Exception {

        setAuthenticatedUser(1L, UserRole.RIDER);

        mockMvc.perform(
                        get("/api/notifications")
                                .param("userId", "2")
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("GET /api/notifications?userId=1&isRead=false - customer can filter own notifications")
    void getNotifications_withIsReadFilter_shouldReturn200() throws Exception {
        setAuthenticatedUser(1L, UserRole.CUSTOMER);
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
    @WithMockUser(roles = "ADMIN")
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
    @WithMockUser(roles = "ADMIN")
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
    @WithMockUser(roles = "ADMIN")
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
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/notifications - message exceeding 255 chars should return 400 Bad Request")
    void createNotification_messageExceeds255_shouldReturn400() throws Exception {
        String longMessage = "a".repeat(256);
        NotificationRequestDto invalid = new NotificationRequestDto(
                1L, NotificationType.ORDER_STATUS_CHANGED, "Title", longMessage
        );

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/notifications - title exceeding 255 chars should return 400 Bad Request")
    void createNotification_titleExceeds255_shouldReturn400() throws Exception {
        String longTitle = "a".repeat(256);
        NotificationRequestDto invalid = new NotificationRequestDto(
                1L, NotificationType.ORDER_STATUS_CHANGED, longTitle, "Valid message"
        );

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @DisplayName("GET /api/notifications/{id}?userId=1 - customer can access own notification")
    void getNotificationById_shouldReturn200() throws Exception {

        setAuthenticatedUser(1L, UserRole.CUSTOMER);

        when(notificationService.getNotificationById(10L, 1L))
                .thenReturn(responseDto);

        mockMvc.perform(
                        get("/api/notifications/{id}", 10L)
                                .param("userId", "1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/notifications/{id} - admin can access any notification without userId")
    void getNotificationById_admin_shouldReturn200() throws Exception {

        when(notificationService.getNotificationById(10L))
                .thenReturn(responseDto);

        mockMvc.perform(
                        get("/api/notifications/{id}", 10L)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));

        verify(notificationService).getNotificationById(10L);
    }

    @Test
    @DisplayName("GET /api/notifications?userId=1 - restaurant owner can access own notifications")
    void getNotifications_restaurantOwner_shouldReturn200() throws Exception {

        setAuthenticatedUser(1L, UserRole.RESTAURANT_OWNER);

        when(notificationService.getNotificationsForUser(1L, null))
                .thenReturn(List.of(responseDto));

        mockMvc.perform(
                        get("/api/notifications")
                                .param("userId", "1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10L));
    }

    @Test
    @DisplayName("GET /api/notifications?userId=1 - rider can access own notifications")
    void getNotifications_rider_shouldReturn200() throws Exception {

        setAuthenticatedUser(1L, UserRole.RIDER);

        when(notificationService.getNotificationsForUser(1L, null))
                .thenReturn(List.of(responseDto));

        mockMvc.perform(
                        get("/api/notifications")
                                .param("userId", "1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10L));
    }

    @Test
    @DisplayName("GET /api/notifications/{id} - customer without userId should return403")
    void getNotificationById_customerWithoutUserId_shouldReturn403() throws Exception {

        setAuthenticatedUser(1L, UserRole.CUSTOMER);

        mockMvc.perform(
                        get("/api/notifications/{id}", 10L)
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("GET /api/notifications/{id} - customer cannot access another user's notification")
    void getNotificationById_otherUser_shouldReturn403() throws Exception {
        setAuthenticatedUser(1L, UserRole.CUSTOMER);

        mockMvc.perform(
                        get("/api/notifications/{id}", 10L)
                                .param("userId", "2")
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("GET /api/notifications/{id}?userId=1 - should return 404 when not found")
    void getNotificationById_notFound_shouldReturn404() throws Exception {

        setAuthenticatedUser(1L, UserRole.CUSTOMER);

        when(notificationService.getNotificationById(99L, 1L))
                .thenThrow(new EntityNotFoundException(
                        "Notification not found with id: 99"
                ));

        mockMvc.perform(
                        get("/api/notifications/{id}", 99L)
                                .param("userId", "1")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("GET /api/notifications/unread-count?userId=1 - customer can access own unread count")
    void getUnreadCount_shouldReturn200() throws Exception {
        setAuthenticatedUser(1L, UserRole.CUSTOMER);
        when(notificationService.getUnreadCount(1L)).thenReturn(new UnreadNotificationCountDto(3L));

        mockMvc.perform(get("/api/notifications/unread-count").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3L));
    }

    @Test
    @DisplayName("GET /api/notifications/unread-count?userId=2 - customer cannot access another user's unread count")
    void getUnreadCount_otherUser_shouldReturn403() throws Exception {

        setAuthenticatedUser(1L, UserRole.CUSTOMER);

        mockMvc.perform(
                        get("/api/notifications/unread-count")
                                .param("userId", "2")
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("PATCH /api/notifications/{id}/read?userId=1 - customer can mark own notification as read")
    void markAsRead_shouldReturn200() throws Exception {
        setAuthenticatedUser(1L, UserRole.CUSTOMER);


        when(notificationService.markAsRead(10L, 1L))
                .thenReturn(responseDto);

        mockMvc.perform(
                        patch("/api/notifications/{id}/read", 10L)
                                .param("userId", "1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/notifications/{id}/read - admin can mark any notification as read without userId")
    void markAsRead_admin_shouldReturn200() throws Exception {

        when(notificationService.markAsRead(10L))
                .thenReturn(responseDto);

        mockMvc.perform(
                        patch("/api/notifications/{id}/read", 10L)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));

        verify(notificationService).markAsRead(10L);
    }

    @Test
    @DisplayName("PATCH /api/notifications/{id}/read - customer cannot mark another user's notification as read")
    void markAsRead_otherUser_shouldReturn403() throws Exception {
        setAuthenticatedUser(1L, UserRole.CUSTOMER);

        mockMvc.perform(
                        patch("/api/notifications/{id}/read", 10L)
                                .param("userId", "2")
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("PATCH /api/notifications/read-all?userId=1 - customer can mark own notifications as read")
    void markAllAsRead_shouldReturn204() throws Exception {
        setAuthenticatedUser(1L, UserRole.CUSTOMER);
        doNothing().when(notificationService).markAllAsRead(1L);

        mockMvc.perform(patch("/api/notifications/read-all").param("userId", "1"))
                .andExpect(status().isNoContent());

        verify(notificationService).markAllAsRead(1L);
    }

    @Test
    @DisplayName("PATCH /api/notifications/read-all?userId=2 - customer cannot mark another user's notifications as read")
    void markAllAsRead_otherUser_shouldReturn403() throws Exception {

        setAuthenticatedUser(1L, UserRole.CUSTOMER);

        mockMvc.perform(
                        patch("/api/notifications/read-all")
                                .param("userId", "2")
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("DELETE /api/notifications/{id}?userId=1 - customer can delete own notification")
    void deleteNotification_shouldReturn204() throws Exception {
        setAuthenticatedUser(1L, UserRole.CUSTOMER);


        doNothing()
                .when(notificationService)
                .deleteNotification(10L, 1L);

        mockMvc.perform(
                        delete("/api/notifications/{id}", 10L)
                                .param("userId", "1")
                )
                .andExpect(status().isNoContent());

        verify(notificationService)
                .deleteNotification(10L, 1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/notifications/{id} - admin can delete any notification without userId")
    void deleteNotification_admin_shouldReturn204() throws Exception {

        doNothing()
                .when(notificationService)
                .deleteNotification(10L);

        mockMvc.perform(
                        delete("/api/notifications/{id}", 10L)
                )
                .andExpect(status().isNoContent());

        verify(notificationService).deleteNotification(10L);
    }

    @Test
    @DisplayName("DELETE /api/notifications/{id} - customer cannot delete another user's notification")
    void deleteNotification_otherUser_shouldReturn403() throws Exception {

        setAuthenticatedUser(1L, UserRole.CUSTOMER);

        mockMvc.perform(
                        delete("/api/notifications/{id}", 10L)
                                .param("userId", "2")
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(notificationService);
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
