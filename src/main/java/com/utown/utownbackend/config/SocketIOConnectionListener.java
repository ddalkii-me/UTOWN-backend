package com.utown.utownbackend.config;

import com.socketio4j.socketio.SocketIOClient;
import com.socketio4j.socketio.SocketIOServer;
import com.utown.utownbackend.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(
        name = "socketio.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SocketIOConnectionListener {

    private final JwtUtil jwtUtil;

    public SocketIOConnectionListener(
            SocketIOServer socketIOServer,
            JwtUtil jwtUtil
    ) {
        this.jwtUtil = jwtUtil;

        socketIOServer.addConnectListener(this::handleConnection);

        socketIOServer.addDisconnectListener(client ->
                log.info("Socket.IO client {} disconnected", client.getSessionId())
        );
    }

    private void handleConnection(SocketIOClient client) {

        String token = client.getHandshakeData()
                .getHttpHeaders()
                .get("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            log.warn("Socket.IO connection rejected: missing Authorization header");
            client.disconnect();
            return;
        }

        token = token.substring(7);

        try {
            if (!jwtUtil.validateToken(token)) {
                log.warn("Socket.IO connection rejected: invalid JWT");
                client.disconnect();
                return;
            }

            Long userId = jwtUtil.extractClaim(
                    token,
                    claims -> claims.get("userId", Long.class)
            );

            if (userId == null) {
                log.warn("Socket.IO connection rejected: JWT has no userId");
                client.disconnect();
                return;
            }

            String room = "user:" + userId;

            client.joinRoom(room);

            log.info(
                    "Socket.IO client {} connected for user {} and joined room {}",
                    client.getSessionId(),
                    userId,
                    room
            );

        } catch (Exception ex) {
            log.warn(
                    "Socket.IO connection rejected: {}",
                    ex.getMessage()
            );
            client.disconnect();
        }
    }
}