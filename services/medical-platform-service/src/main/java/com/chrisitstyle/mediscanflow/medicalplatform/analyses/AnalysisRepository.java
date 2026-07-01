package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisListItemDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.RecentAnalysisDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.dashboard.dto.AnalysisStatusCountDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AnalysisRepository extends JpaRepository<Analysis, UUID> {
    List<Analysis> findByPatientIdOrderByCreatedAtDesc(UUID patientId);
    long countByStatus(AnalysisStatus status);

    @Query("""
            SELECT new com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.RecentAnalysisDTO(
                analysis.id,
                patient.id,
                CONCAT(patient.firstName, ' ', patient.lastName),
                analysis.status,
                analysis.originalFileName,
                analysis.modelName,
                analysis.modelVersion,
                analysis.fileSizeBytes,
                analysis.createdAt,
                analysis.completedAt
            )
            FROM Analysis analysis
            JOIN analysis.patient patient
            ORDER BY analysis.createdAt DESC
            """)
    List<RecentAnalysisDTO> findRecentAnalyses(Pageable pageable);

    @Query("""
        SELECT new com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisListItemDTO(
            analysis.id,
            patient.id,
            CONCAT(patient.firstName, ' ', patient.lastName),
            analysis.status,
            analysis.originalFileName,
            analysis.modelName,
            analysis.modelVersion,
            analysis.fileSizeBytes,
            analysis.createdAt,
            analysis.completedAt
        )
        FROM Analysis analysis
        JOIN analysis.patient patient
        ORDER BY analysis.createdAt DESC
        """)
    List<AnalysisListItemDTO> findAllAnalysisListItems();

    @Query("""
        select new com.chrisitstyle.mediscanflow.medicalplatform.dashboard.dto.AnalysisStatusCountDTO(
            a.status,
            count(a)
        )
        from Analysis a
        group by a.status
        """)
    List<AnalysisStatusCountDTO> countAnalysesByStatus();

    List<Analysis> findByCreatedAtGreaterThanEqual(Instant createdAt);
}
