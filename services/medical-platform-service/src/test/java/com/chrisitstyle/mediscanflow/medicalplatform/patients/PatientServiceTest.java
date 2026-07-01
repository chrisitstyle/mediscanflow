package com.chrisitstyle.mediscanflow.medicalplatform.patients;

import com.chrisitstyle.mediscanflow.medicalplatform.patients.dto.PatientResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientService patientService;

    @Test
    void findAllReturnsAllPatientsWhenSearchIsNull() {
        Patient patient = patient();

        when(patientRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(patient));

        List<PatientResponseDTO> patients = patientService.findAll(null);

        assertEquals(1, patients.size());
        assertPatientResponse(patients.getFirst());

        verify(patientRepository).findAllByOrderByCreatedAtDesc();
        verify(patientRepository, never()).searchByText(anyString());
    }

    @Test
    void findAllReturnsAllPatientsWhenSearchIsBlank() {
        Patient patient = patient();

        when(patientRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(patient));

        List<PatientResponseDTO> patients = patientService.findAll("   ");

        assertEquals(1, patients.size());
        assertPatientResponse(patients.getFirst());

        verify(patientRepository).findAllByOrderByCreatedAtDesc();
        verify(patientRepository, never()).searchByText(anyString());
    }

    @Test
    void findAllSearchesPatientsWhenSearchHasText() {
        Patient patient = patient();

        when(patientRepository.searchByText("john"))
                .thenReturn(List.of(patient));

        List<PatientResponseDTO> patients = patientService.findAll("  john  ");

        assertEquals(1, patients.size());
        assertPatientResponse(patients.getFirst());

        verify(patientRepository).searchByText("john");
        verify(patientRepository, never()).findAllByOrderByCreatedAtDesc();
    }

    private static Patient patient() {
        return Patient.create(
                "John",
                "Doe",
                LocalDate.parse("1990-01-15"),
                "MRN-001"
        );
    }

    private static void assertPatientResponse(PatientResponseDTO patient) {
        assertAll(
                () -> assertEquals("John", patient.firstName()),
                () -> assertEquals("Doe", patient.lastName()),
                () -> assertEquals(LocalDate.parse("1990-01-15"), patient.dateOfBirth()),
                () -> assertEquals("MRN-001", patient.medicalRecordNumber())
        );
    }
}