package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.AnalysisListItemDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.dto.RecentAnalysisDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.projection.AnalysisStatusCountProjection;
import com.chrisitstyle.mediscanflow.medicalplatform.analyses.projection.AnalysisSummaryProjection;
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

    List<AnalysisSummaryProjection> findAllByOrderByCreatedAtDesc();

    List<AnalysisSummaryProjection> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
        select
            analysis.status as status,
            count(analysis) as count
        from Analysis analysis
        group by analysis.status
        """)
    List<AnalysisStatusCountProjection> countAnalysesByStatus();

    List<Analysis> findByCreatedAtGreaterThanEqual(Instant createdAt);
}
