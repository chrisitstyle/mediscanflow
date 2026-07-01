package com.chrisitstyle.mediscanflow.medicalplatform;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Requires infrastructure services such as PostgreSQL/Flyway;" +
		"covered by lightweight MVC/service tests for now")
@SpringBootTest
class MedicalPlatformServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
