package com.chrisitstyle.mediscanflow.medicalplatform.patients;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface PatientRepository
        extends JpaRepository<Patient, UUID>, JpaSpecificationExecutor<Patient> {

    Optional<Patient> findByMedicalRecordNumber(String medicalRecordNumber);

    boolean existsByMedicalRecordNumber(String medicalRecordNumber);
}