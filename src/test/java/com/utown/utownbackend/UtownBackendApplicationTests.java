package com.utown.utownbackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "aws.s3.enabled=false")
class UtownBackendApplicationTests {

	@Test
	void contextLoadsWithS3Disabled() {
	}
}
