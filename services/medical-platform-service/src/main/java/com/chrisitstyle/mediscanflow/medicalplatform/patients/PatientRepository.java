package com.chrisitstyle.mediscanflow.medicalplatform.patients;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {
    boolean existsByMedicalRecordNumber(String medicalRecordNumber);
}
