package com.chrisitstyle.mediscanflow.medicalplatform;

import com.chrisitstyle.mediscanflow.medicalplatform.auth.keycloak.KeycloakAdminProperties;
import com.chrisitstyle.mediscanflow.medicalplatform.common.validation.FileUploadValidationProperties;
import com.chrisitstyle.mediscanflow.medicalplatform.storage.MinioProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties({
        MinioProperties.class,
        FileUploadValidationProperties.class,
        KeycloakAdminProperties.class
})
public class MedicalPlatformServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(MedicalPlatformServiceApplication.class, args);
    }

}
