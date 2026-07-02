package com.chrisitstyle.mediscanflow.medicalplatform.audit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    List<AuditEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<AuditEvent> findByPatientIdOrderByCreatedAtDesc(UUID patientId, Pageable pageable);

    List<AuditEvent> findByAnalysisIdOrderByCreatedAtDesc(UUID analysisId, Pageable pageable);
}
