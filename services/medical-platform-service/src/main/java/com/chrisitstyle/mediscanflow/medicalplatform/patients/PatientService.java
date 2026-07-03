package com.chrisitstyle.mediscanflow.medicalplatform.patients;

import com.chrisitstyle.mediscanflow.medicalplatform.audit.AuditEventService;
import com.chrisitstyle.mediscanflow.medicalplatform.audit.AuditEventType;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.DuplicateResourceException;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.ResourceNotFoundException;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.dto.CreatePatientRequestDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.dto.PatientProfileUpdateDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.dto.PatientResponseDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.mapper.PatientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientService {

    private static final String PATIENT_NOT_FOUND_WITH_ID_MSG = "Patient not found with id: ";

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
                patientCreatedMessage(savedPatient)
        );

        return PatientMapper.toResponseDTO(savedPatient);
    }

    @Transactional(readOnly = true)
    public PatientResponseDTO findById(UUID id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PATIENT_NOT_FOUND_WITH_ID_MSG + id));

        return PatientMapper.toResponseDTO(patient);
    }

    @Transactional(readOnly = true)
    public List<PatientResponseDTO> findAll(String search, boolean includeArchived) {
        Specification<Patient> specification = Specification.allOf(
                PatientSpecifications.archiveFilter(includeArchived),
                PatientSpecifications.textSearch(search)
        );

        return patientRepository.findAll(
                        specification,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                )
                .stream()
                .map(PatientMapper::toResponseDTO)
                .toList();
    }

    @Transactional
    public PatientResponseDTO updatePatientProfile(UUID patientId, PatientProfileUpdateDTO request) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException(PATIENT_NOT_FOUND_WITH_ID_MSG + patientId));

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
                patientProfileUpdatedMessage(savedPatient)
        );

        return PatientMapper.toResponseDTO(savedPatient);
    }

    @Transactional
    public PatientResponseDTO archivePatient(UUID patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException(PATIENT_NOT_FOUND_WITH_ID_MSG + patientId));

        patient.archive();

        auditEventService.recordEvent(
                AuditEventType.PATIENT_ARCHIVED,
                patient.getId(),
                null,
                patientArchivedMessage(patient)
        );

        return PatientMapper.toResponseDTO(patient);
    }

    @Transactional
    public PatientResponseDTO restorePatient(UUID patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException(PATIENT_NOT_FOUND_WITH_ID_MSG + patientId));

        patient.restore();

        auditEventService.recordEvent(
                AuditEventType.PATIENT_RESTORED,
                patient.getId(),
                null,
                patientRestoredMessage(patient)
        );

        return PatientMapper.toResponseDTO(patient);
    }

    private String patientCreatedMessage(Patient patient) {
        return "Patient %s was created.".formatted(formatPatientName(patient));
    }

    private String patientProfileUpdatedMessage(Patient patient) {
        return "Patient %s profile was updated.".formatted(formatPatientName(patient));
    }

    private String patientArchivedMessage(Patient patient) {
        return "Patient %s was archived.".formatted(formatPatientName(patient));
    }

    private String patientRestoredMessage(Patient patient) {
        return "Patient %s was restored.".formatted(formatPatientName(patient));
    }

    private String formatPatientName(Patient patient) {
        return "%s %s".formatted(patient.getFirstName(), patient.getLastName());
    }
}