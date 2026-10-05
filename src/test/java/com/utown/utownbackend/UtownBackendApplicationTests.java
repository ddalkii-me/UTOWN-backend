package com.utown.utownbackend;

import com.utown.utownbackend.service.SocketIONotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "aws.s3.enabled=false")
class UtownBackendApplicationTests {

	@MockitoBean
	private SocketIONotificationService socketIONotificationService;

	@Test
	void contextLoadsWithS3Disabled() {
	}
}
