package com.chrisitstyle.mediscanflow.medicalplatform.system;

import com.chrisitstyle.mediscanflow.medicalplatform.system.dto.SystemStatusResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
class SystemStatusController {

    private final SystemStatusService systemStatusService;

    @GetMapping("/status")
    SystemStatusResponseDTO getStatus() {
        return systemStatusService.getStatus();
    }
}
