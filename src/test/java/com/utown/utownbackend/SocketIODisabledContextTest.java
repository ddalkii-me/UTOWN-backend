package com.utown.utownbackend;

import com.socketio4j.socketio.SocketIOServer;
import com.utown.utownbackend.service.SocketIONotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = "socketio.enabled=false")
class SocketIODisabledContextTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SocketIONotificationService notificationService;

    @Test
    void contextStartsWithoutSocketIOServer() {
        assertThat(applicationContext.getBeansOfType(SocketIOServer.class))
                .isEmpty();

        assertThat(notificationService).isNotNull();
    }
}
