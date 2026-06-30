package com.chrisitstyle.mediscanflow.medicalplatform;

import com.chrisitstyle.mediscanflow.medicalplatform.storage.MinioProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MinioProperties.class)
public class MedicalPlatformServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MedicalPlatformServiceApplication.class, args);
	}

}
