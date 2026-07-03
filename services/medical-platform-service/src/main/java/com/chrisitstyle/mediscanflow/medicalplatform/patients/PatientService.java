package com.chrisitstyle.mediscanflow.medicalplatform.patients;

import com.chrisitstyle.mediscanflow.medicalplatform.audit.AuditEventService;
import com.chrisitstyle.mediscanflow.medicalplatform.audit.AuditEventType;
import com.chrisitstyle.mediscanflow.medicalplatform.common.error.DuplicateResourceException;
import com.chrisitstyle.mediscanflow.medicalplatform.common.error.ResourceNotFoundException;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.dto.CreatePatientRequestDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.dto.PatientProfileUpdateDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.dto.PatientResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientService {

    private static final String PATIENT_AUDIT_PREFIX = "Patient ";
    private static final String PATIENT_NOT_FOUND_WITH_ID_MESSAGE = "Patient not found with id: ";

    private final PatientRepository patientRepository;
    private final AuditEventService auditEventService;

    @Transactional
    public PatientResponseDTO create(CreatePatientRequestDTO request) {
        if (patientRepository.existsByMedicalRecordNumber(request.medicalRecordNumber())) {
            throw new DuplicateResourceException(
                    "Patient with medical record number already exists"
            );
        }

        Patient patient = Patient.create(
                request.firstName(),
                request.lastName(),
                request.dateOfBirth(),
                request.medicalRecordNumber()
        );

        Patient savedPatient = patientRepository.save(patient);

        auditEventService.recordEvent(
                AuditEventType.PATIENT_CREATED,
                savedPatient.getId(),
                null,
                PATIENT_AUDIT_PREFIX + formatPatientName(savedPatient) + " was created."
        );

        return toResponseDTO(savedPatient);
    }

    @Transactional(readOnly = true)
    public List<PatientResponseDTO> findAll(String search, boolean includeArchived) {
        String normalizedSearch = search == null ? null : search.trim();

        List<Patient> patients;

        if (normalizedSearch == null || normalizedSearch.isBlank()) {
            patients = includeArchived
                    ? patientRepository.findAllByOrderByCreatedAtDesc()
                    : patientRepository.findAllByArchivedFalseOrderByCreatedAtDesc();
        } else {
            patients = includeArchived
                    ? patientRepository.searchByText(normalizedSearch)
                    : patientRepository.searchActiveByText(normalizedSearch);
        }

        return patients.stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional
    public PatientResponseDTO updatePatientProfile(UUID patientId, PatientProfileUpdateDTO request) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException(PATIENT_NOT_FOUND_WITH_ID_MESSAGE + patientId));

        patient.updateProfile(
                request.firstName().trim(),
                request.lastName().trim(),
                request.dateOfBirth()
        );

        Patient savedPatient = patientRepository.save(patient);

        auditEventService.recordEvent(
                AuditEventType.PATIENT_PROFILE_UPDATED,
                savedPatient.getId(),
                null,
                PATIENT_AUDIT_PREFIX + formatPatientName(savedPatient) + " profile was updated."
        );

        return toResponseDTO(savedPatient);
    }

    @Transactional
    public PatientResponseDTO archivePatient(UUID patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException(PATIENT_NOT_FOUND_WITH_ID_MESSAGE + patientId));

        patient.archive();

        auditEventService.recordEvent(
                AuditEventType.PATIENT_ARCHIVED,
                patient.getId(),
                null,
                PATIENT_AUDIT_PREFIX + formatPatientName(patient) + " was archived."
        );

        return toResponseDTO(patient);
    }

    @Transactional
    public PatientResponseDTO restorePatient(UUID patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException(PATIENT_NOT_FOUND_WITH_ID_MESSAGE + patientId));

        patient.restore();

        auditEventService.recordEvent(
                AuditEventType.PATIENT_RESTORED,
                patient.getId(),
                null,
                PATIENT_AUDIT_PREFIX + formatPatientName(patient) + " was restored."
        );

        return toResponseDTO(patient);
    }

    @Transactional(readOnly = true)
    public PatientResponseDTO findById(UUID id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        return toResponseDTO(patient);
    }

    private PatientResponseDTO toResponseDTO(Patient patient) {
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

    private String formatPatientName(Patient patient) {
        return patient.getFirstName() + " " + patient.getLastName();
    }
}