package com.utown.utownbackend.service;

import java.util.Map;

import com.socketio4j.socketio.SocketIOServer;
import com.utown.utownbackend.util.SocketIOEvents;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
public class SocketIONotificationService {

    private final SocketIOServer socketIOServer;
    private final ObjectMapper objectMapper;

    public SocketIONotificationService(
            ObjectProvider<SocketIOServer> socketIOServerProvider,
            ObjectMapper objectMapper
    ) {
        this.socketIOServer = socketIOServerProvider.getIfAvailable();
        this.objectMapper = objectMapper;
    }

    public void sendNotification(Long userId, Object notification) {
        sendToUser(userId, SocketIOEvents.NOTIFICATION, notification);
    }

    public void sendOrderStatusUpdated(Long userId, Object notification) {
        sendToUser(userId, SocketIOEvents.ORDER_STATUS_UPDATED, notification);
    }

    public void sendNewOrder(Long restaurantOwnerId, Object notification) {
        sendToUser(restaurantOwnerId, SocketIOEvents.NEW_ORDER, notification);
    }

    private void sendToUser(Long userId, String event, Object data) {
        if (socketIOServer == null || userId == null || data == null) {
            return;
        }

        String room = "user:" + userId;

        try {
            Map<String, Object> payload =
                    objectMapper.convertValue(data, Map.class);

            socketIOServer
                    .getRoomOperations(room)
                    .sendEvent(event, payload);
        } catch (Exception ex) {
            log.warn(
                    "Failed to send Socket.IO event '{}' to user {}: {}",
                    event,
                    userId,
                    ex.getMessage()
            );
        }
    }
}
