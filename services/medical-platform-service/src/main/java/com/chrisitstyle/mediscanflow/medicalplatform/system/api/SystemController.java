package com.chrisitstyle.mediscanflow.medicalplatform.system.api;

import com.chrisitstyle.mediscanflow.medicalplatform.system.api.dto.SystemStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SystemController {

    @GetMapping("/system/status")
    SystemStatusResponse status() {
        return new SystemStatusResponse("medical-platform-service", "UP");
    }
}
