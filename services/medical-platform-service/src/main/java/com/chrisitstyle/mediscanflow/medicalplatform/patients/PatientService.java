package com.chrisitstyle.mediscanflow.medicalplatform.patients;

import com.chrisitstyle.mediscanflow.medicalplatform.common.error.DuplicateResourceException;
import com.chrisitstyle.mediscanflow.medicalplatform.common.error.ResourceNotFoundException;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.dto.CreatePatientRequestDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.dto.PatientProfileUpdateDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.dto.PatientResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

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

        return toResponseDTO(savedPatient);
    }

    @Transactional(readOnly = true)
    public List<PatientResponseDTO> findAll(String search) {
        List<Patient> patients = StringUtils.hasText(search)
                ? patientRepository.searchByText(search.trim())
                : patientRepository.findAllByOrderByCreatedAtDesc();

        return patients.stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional
    public PatientResponseDTO updatePatientProfile(UUID patientId, PatientProfileUpdateDTO request) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));

        patient.updateProfile(
                request.firstName().trim(),
                request.lastName().trim(),
                request.dateOfBirth()
        );

        return toResponseDTO(patientRepository.save(patient));
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
                patient.getCreatedAt()
        );
    }
}