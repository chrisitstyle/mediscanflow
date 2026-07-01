package com.chrisitstyle.mediscanflow.medicalplatform.system.dto;

import java.util.Map;

public record SystemStatusResponseDTO(
        String status,
        Map<String, SystemComponentStatusDTO> components
) {
}
