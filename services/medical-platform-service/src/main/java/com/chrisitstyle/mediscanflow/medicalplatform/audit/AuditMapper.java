package com.chrisitstyle.mediscanflow.medicalplatform.audit;

import com.chrisitstyle.mediscanflow.medicalplatform.audit.dto.AuditEventDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuditMapper {

    public AuditEventDTO toDTO(AuditEvent event) {
        return new AuditEventDTO(
                event.getId(),
                event.getType(),
                event.getActorUserId(),
                event.getActorEmail(),
                event.getActorRole(),
                event.getPatientId(),
                event.getAnalysisId(),
                event.getMessage(),
                event.getMetadata(),
                event.getCreatedAt()
        );
    }

    public List<AuditEventDTO> toDTOs(List<AuditEvent> events) {
        return events.stream()
                .map(this::toDTO)
                .toList();
    }
}
