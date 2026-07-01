package com.chrisitstyle.mediscanflow.medicalplatform.patients;

import com.chrisitstyle.mediscanflow.medicalplatform.common.error.ResourceNotFoundException;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.dto.PatientProfileUpdateDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.dto.PatientResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void updatePatientProfileUpdatesEditableFieldsOnly() {
        UUID patientId = UUID.randomUUID();

        Patient patient = patient(
                "John",
                "Smith",
                LocalDate.of(1985, Month.JANUARY, 10),
                "MRN-00001"
        );

        PatientProfileUpdateDTO request = new PatientProfileUpdateDTO(
                " Anna ",
                " Kowalska ",
                LocalDate.of(1990, Month.APRIL, 15)
        );

        when(patientRepository.findById(patientId))
                .thenReturn(Optional.of(patient));

        when(patientRepository.save(patient))
                .thenReturn(patient);

        PatientResponseDTO response = patientService.updatePatientProfile(patientId, request);

        assertAll(
                () -> assertEquals("Anna", response.firstName()),
                () -> assertEquals("Kowalska", response.lastName()),
                () -> assertEquals(LocalDate.of(1990, Month.APRIL, 15), response.dateOfBirth()),
                () -> assertEquals("MRN-00001", response.medicalRecordNumber())
        );

        verify(patientRepository).findById(patientId);
        verify(patientRepository).save(patient);
    }

    @Test
    void updatePatientProfileThrowsWhenPatientDoesNotExist() {
        UUID patientId = UUID.randomUUID();

        PatientProfileUpdateDTO request = new PatientProfileUpdateDTO(
                "Anna",
                "Kowalska",
                LocalDate.of(1990, Month.APRIL, 15)
        );

        when(patientRepository.findById(patientId))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> patientService.updatePatientProfile(patientId, request)
        );

        assertEquals("Patient not found with id: " + patientId, exception.getMessage());

        verify(patientRepository).findById(patientId);
    }

    private static Patient patient() {
        return patient(
                "John",
                "Doe",
                LocalDate.parse("1990-01-15"),
                "MRN-001"
        );
    }

    private static Patient patient(
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            String medicalRecordNumber
    ) {
        return Patient.create(
                firstName,
                lastName,
                dateOfBirth,
                medicalRecordNumber
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