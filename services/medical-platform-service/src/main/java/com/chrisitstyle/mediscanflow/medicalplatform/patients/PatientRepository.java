package com.chrisitstyle.mediscanflow.medicalplatform.patients;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByMedicalRecordNumber(String medicalRecordNumber);

    boolean existsByMedicalRecordNumber(String medicalRecordNumber);

    List<Patient> findAllByOrderByCreatedAtDesc();

    @Query("""
            SELECT patient
            FROM Patient patient
            WHERE LOWER(patient.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(patient.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(patient.medicalRecordNumber) LIKE LOWER(CONCAT('%', :search, '%'))
            ORDER BY patient.createdAt DESC
            """)
    List<Patient> searchByText(@Param("search") String search);
}