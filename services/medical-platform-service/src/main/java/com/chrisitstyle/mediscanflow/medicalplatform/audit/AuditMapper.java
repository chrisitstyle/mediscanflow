package com.chrisitstyle.mediscanflow.medicalplatform.audit;

import com.chrisitstyle.mediscanflow.medicalplatform.audit.dto.AuditEventDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.audit.dto.AuditEventPageDTO;
import org.springframework.data.domain.Page;
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

    public AuditEventPageDTO toPageDTO(Page<AuditEvent> page) {
        return new AuditEventPageDTO(
                toDTOs(page.getContent()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
