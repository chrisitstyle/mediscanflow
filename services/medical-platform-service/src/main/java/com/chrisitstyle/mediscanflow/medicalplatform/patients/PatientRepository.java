package com.chrisitstyle.mediscanflow.medicalplatform.patients;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByMedicalRecordNumber(String medicalRecordNumber);

    boolean existsByMedicalRecordNumber(String medicalRecordNumber);

    @Query("""
        select p from Patient p
        where :includeArchived = true or p.archived = false
        order by p.createdAt desc
        """)
    List<Patient> findAllByArchiveFilter(@Param("includeArchived") boolean includeArchived);

    default List<Patient> searchByText(String search, boolean includeArchived) {
        return searchByPattern(
                "%" + search.toLowerCase(Locale.ROOT) + "%",
                includeArchived
        );
    }

    @Query("""
        select p from Patient p
        where (:includeArchived = true or p.archived = false)
          and (
            lower(p.firstName) like :searchPattern
            or lower(p.lastName) like :searchPattern
            or lower(p.medicalRecordNumber) like :searchPattern
          )
        order by p.createdAt desc
        """)
    List<Patient> searchByPattern(
            @Param("searchPattern") String searchPattern,
            @Param("includeArchived") boolean includeArchived
    );
}