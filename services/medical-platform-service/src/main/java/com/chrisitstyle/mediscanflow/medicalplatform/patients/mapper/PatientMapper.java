package com.chrisitstyle.mediscanflow.medicalplatform.patients.mapper;

import com.chrisitstyle.mediscanflow.medicalplatform.patients.Patient;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.dto.PatientResponseDTO;

public final class PatientMapper {

    private PatientMapper() {
        throw new IllegalStateException("Utility class");
    }

    public static PatientResponseDTO toResponseDTO(Patient patient) {
        return new PatientResponseDTO(
                patient.getId(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getDateOfBirth(),
                patient.getMedicalRecordNumber(),
                patient.getCreatedAt(),
                patient.isArchived(),
                patient.getArchivedAt()
        );
    }
}