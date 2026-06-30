package com.chrisitstyle.mediscanflow.medicalplatform.analyses;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "analysis_detections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalysisDetection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @Column(name = "detection_label", nullable = false, length = 100)
    private String label;

    @Column(nullable = false)
    private double confidence;

    @Column(name = "x_coordinate", nullable = false)
    private double x;

    @Column(name = "y_coordinate", nullable = false)
    private double y;

    @Column(nullable = false)
    private double width;

    @Column(nullable = false)
    private double height;

    private AnalysisDetection(
            Analysis analysis,
            String label,
            double confidence,
            double x,
            double y,
            double width,
            double height
    ) {
        this.analysis = analysis;
        this.label = label;
        this.confidence = confidence;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    static AnalysisDetection create(
            Analysis analysis,
            String label,
            double confidence,
            double x,
            double y,
            double width,
            double height
    ) {
        return new AnalysisDetection(
                analysis,
                label,
                confidence,
                x,
                y,
                width,
                height
        );
    }
}
