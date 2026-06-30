package com.chrisitstyle.mediscanflow.medicalplatform.patients.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PatientResponseDTO(
        UUID id,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String medicalRecordNumber,
        Instant createdAt
) {
}
