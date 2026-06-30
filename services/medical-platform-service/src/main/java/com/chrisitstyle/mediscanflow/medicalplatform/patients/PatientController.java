package com.chrisitstyle.mediscanflow.medicalplatform.patients;

import com.chrisitstyle.mediscanflow.medicalplatform.patients.dto.CreatePatientRequestDTO;
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
    List<PatientResponseDTO> findAll() {
        return patientService.findAll();
    }

    @GetMapping("/{id}")
    PatientResponseDTO findById(@PathVariable UUID id) {
        return patientService.findById(id);
    }
}
