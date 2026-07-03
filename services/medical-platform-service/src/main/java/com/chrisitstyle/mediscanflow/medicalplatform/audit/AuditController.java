package com.chrisitstyle.mediscanflow.medicalplatform.audit;

import com.chrisitstyle.mediscanflow.medicalplatform.audit.dto.AuditEventDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.audit.dto.AuditEventPageDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class AuditController {

    private final AuditEventService auditEventService;

    public AuditController(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @GetMapping("/audit-events")
    public AuditEventPageDTO getAuditEvents(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return auditEventService.getEvents(page, size);
    }

    @GetMapping("/audit-events/recent")
    public List<AuditEventDTO> getRecentAuditEvents(
            @RequestParam(required = false) Integer limit
    ) {
        return auditEventService.getRecentEvents(limit);
    }

    @GetMapping("/patients/{patientId}/audit-events")
    public List<AuditEventDTO> getPatientAuditEvents(
            @PathVariable UUID patientId,
            @RequestParam(required = false) Integer limit
    ) {
        return auditEventService.getPatientEvents(patientId, limit);
    }

    @GetMapping("/analyses/{analysisId}/audit-events")
    public List<AuditEventDTO> getAnalysisAuditEvents(
            @PathVariable UUID analysisId,
            @RequestParam(required = false) Integer limit
    ) {
        return auditEventService.getAnalysisEvents(analysisId, limit);
    }
}
