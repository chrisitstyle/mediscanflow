package com.chrisitstyle.mediscanflow.medicalplatform.audit;

import com.chrisitstyle.mediscanflow.medicalplatform.audit.dto.AuditEventDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.auth.AuthenticatedUserProvider;
import com.chrisitstyle.mediscanflow.medicalplatform.auth.dto.CurrentUserDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuditEventService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final AuditEventRepository auditEventRepository;
    private final AuditMapper auditMapper;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public AuditEventService(
            AuditEventRepository auditEventRepository,
            AuditMapper auditMapper,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.auditEventRepository = auditEventRepository;
        this.auditMapper = auditMapper;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @Transactional
    public void recordEvent(
            AuditEventType type,
            UUID patientId,
            UUID analysisId,
            String message
    ) {
        saveEvent(type, patientId, analysisId, message, null);
    }

    @Transactional
    public void recordEventWithMetadata(
            AuditEventType type,
            UUID patientId,
            UUID analysisId,
            String message,
            String metadata
    ) {
        saveEvent(type, patientId, analysisId, message, metadata);
    }

    private void saveEvent(
            AuditEventType type,
            UUID patientId,
            UUID analysisId,
            String message,
            String metadata
    ) {
        CurrentUserDTO currentUser = authenticatedUserProvider.getCurrentUser();

        AuditEvent event = new AuditEvent();
        event.setType(type);
        event.setActorUserId(currentUser.id());
        event.setActorEmail(currentUser.email());
        event.setActorRole(currentUser.roles().stream().findFirst().orElse(null));
        event.setPatientId(patientId);
        event.setAnalysisId(analysisId);
        event.setMessage(message);
        event.setMetadata(metadata);

        auditEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AuditEventDTO> getRecentEvents(Integer limit) {
        int resolvedLimit = resolveLimit(limit);

        return auditMapper.toDTOs(
                auditEventRepository.findAllByOrderByCreatedAtDesc(
                        PageRequest.of(0, resolvedLimit)
                )
        );
    }

    @Transactional(readOnly = true)
    public List<AuditEventDTO> getPatientEvents(UUID patientId, Integer limit) {
        int resolvedLimit = resolveLimit(limit);

        return auditMapper.toDTOs(
                auditEventRepository.findByPatientIdOrderByCreatedAtDesc(
                        patientId,
                        PageRequest.of(0, resolvedLimit)
                )
        );
    }

    @Transactional(readOnly = true)
    public List<AuditEventDTO> getAnalysisEvents(UUID analysisId, Integer limit) {
        int resolvedLimit = resolveLimit(limit);

        return auditMapper.toDTOs(
                auditEventRepository.findByAnalysisIdOrderByCreatedAtDesc(
                        analysisId,
                        PageRequest.of(0, resolvedLimit)
                )
        );
    }

    private int resolveLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }

        return Math.clamp(limit, 1, MAX_LIMIT);
    }
}