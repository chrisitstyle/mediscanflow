package com.chrisitstyle.mediscanflow.medicalplatform.analyses.projection;

import com.chrisitstyle.mediscanflow.medicalplatform.analyses.AnalysisStatus;

public interface AnalysisStatusCountProjection {

    AnalysisStatus getStatus();
    long getCount();
}
