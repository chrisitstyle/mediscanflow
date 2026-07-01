package com.chrisitstyle.mediscanflow.medicalplatform.patients;

import com.chrisitstyle.mediscanflow.medicalplatform.patients.dto.CreatePatientRequestDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.dto.PatientProfileUpdateDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.dto.PatientResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
class PatientController {

    private final PatientService patientService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PatientResponseDTO create(@Valid @RequestBody CreatePatientRequestDTO request) {
        return patientService.create(request);
    }

    @GetMapping
    public List<PatientResponseDTO> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean includeArchived
    ) {
        return patientService.findAll(search, includeArchived);
    }

    @GetMapping("/{id}")
    PatientResponseDTO findById(@PathVariable UUID id) {
        return patientService.findById(id);
    }

    @PutMapping("/{patientId}/profile")
    public PatientResponseDTO updatePatientProfile(
            @PathVariable UUID patientId,
            @Valid @RequestBody PatientProfileUpdateDTO request
    ) {
        return patientService.updatePatientProfile(patientId, request);
    }

    @PatchMapping("/{patientId}/archive")
    public PatientResponseDTO archivePatient(@PathVariable UUID patientId) {
        return patientService.archivePatient(patientId);
    }

    @PatchMapping("/{patientId}/restore")
    public PatientResponseDTO restorePatient(@PathVariable UUID patientId) {
        return patientService.restorePatient(patientId);
    }
}
