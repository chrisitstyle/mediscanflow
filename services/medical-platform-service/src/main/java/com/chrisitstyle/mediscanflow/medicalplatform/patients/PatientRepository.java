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
    List<Patient> findAllByArchivedFalseOrderByCreatedAtDesc();
    @Query("""
        select p from Patient p
        where p.firstName like concat('%', :search, '%')
           or p.lastName like concat('%', :search, '%')
           or p.medicalRecordNumber like concat('%', :search, '%')
        order by p.createdAt desc
        """)
    List<Patient> searchByText(@Param("search") String search);

    @Query("""
        select p from Patient p
        where p.archived = false
          and (
            p.firstName like concat('%', :search, '%')
            or p.lastName like concat('%', :search, '%')
            or p.medicalRecordNumber like concat('%', :search, '%')
          )
        order by p.createdAt desc
        """)
    List<Patient> searchActiveByText(@Param("search") String search);
}