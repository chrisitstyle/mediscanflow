package com.chrisitstyle.mediscanflow.medicalplatform.audit.dto;

import java.util.List;

public record AuditEventPageDTO(
        List<AuditEventDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
