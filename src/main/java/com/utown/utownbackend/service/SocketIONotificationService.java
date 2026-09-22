package com.utown.utownbackend.service;

import com.socketio4j.socketio.SocketIOServer;
import com.utown.utownbackend.util.SocketIOEvents;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        name = "socketio.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SocketIONotificationService {

    private final SocketIOServer socketIOServer;

    public SocketIONotificationService(SocketIOServer socketIOServer) {
        this.socketIOServer = socketIOServer;
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
        if (userId == null || data == null) {
            return;
        }

        String room = "user:" + userId;

        socketIOServer
                .getRoomOperations(room)
                .sendEvent(event, data);
    }
}