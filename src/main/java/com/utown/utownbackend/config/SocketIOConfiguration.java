package com.utown.utownbackend.config;

import com.socketio4j.socketio.Configuration;
import com.socketio4j.socketio.SocketIOServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "socketio.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SocketIOConfiguration {

    @Value("${socketio.host:127.0.0.1}")
    private String host;

    @Value("${socketio.port:9092}")
    private int port;

    @Bean(initMethod = "start", destroyMethod = "stop")
    public SocketIOServer socketIOServer() {

        Configuration config = new Configuration();

        config.setHostname(host);
        config.setPort(port);

        return new SocketIOServer(config);
    }
}