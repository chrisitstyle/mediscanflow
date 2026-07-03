package com.chrisitstyle.mediscanflow.medicalplatform.patients;

import com.chrisitstyle.mediscanflow.medicalplatform.audit.AuditEventService;
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

import static com.chrisitstyle.mediscanflow.medicalplatform.testentities.PatientTestEntity.patient;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AuditEventService auditEventService;

    @InjectMocks
    private PatientService patientService;

    @Test
    void findAllReturnsActivePatientsWhenSearchIsNull() {
        Patient patient = patient();

        when(patientRepository.findAllByArchiveFilter(false))
                .thenReturn(List.of(patient));

        List<PatientResponseDTO> patients = patientService.findAll(null, false);

        assertEquals(1, patients.size());
        assertPatientResponse(patients.getFirst());

        verify(patientRepository).findAllByArchiveFilter(false);
        verify(patientRepository, never()).searchByText(anyString(), anyBoolean());
    }

    @Test
    void findAllReturnsActivePatientsWhenSearchIsBlank() {
        Patient patient = patient();

        when(patientRepository.findAllByArchiveFilter(false))
                .thenReturn(List.of(patient));

        List<PatientResponseDTO> patients = patientService.findAll("   ", false);

        assertEquals(1, patients.size());
        assertPatientResponse(patients.getFirst());

        verify(patientRepository).findAllByArchiveFilter(false);
        verify(patientRepository, never()).searchByText(anyString(), anyBoolean());
    }

    @Test
    void findAllSearchesActivePatientsWhenSearchHasText() {
        Patient patient = patient();

        when(patientRepository.searchByText("john", false))
                .thenReturn(List.of(patient));

        List<PatientResponseDTO> patients = patientService.findAll("  john  ", false);

        assertEquals(1, patients.size());
        assertPatientResponse(patients.getFirst());

        verify(patientRepository).searchByText("john", false);
        verify(patientRepository, never()).findAllByArchiveFilter(anyBoolean());
    }

    @Test
    void findAllReturnsAllPatientsWhenIncludeArchivedIsTrueAndSearchIsNull() {
        Patient patient = patient();

        when(patientRepository.findAllByArchiveFilter(true))
                .thenReturn(List.of(patient));

        List<PatientResponseDTO> patients = patientService.findAll(null, true);

        assertEquals(1, patients.size());
        assertPatientResponse(patients.getFirst());

        verify(patientRepository).findAllByArchiveFilter(true);
        verify(patientRepository, never()).searchByText(anyString(), anyBoolean());
    }

    @Test
    void findAllSearchesAllPatientsWhenIncludeArchivedIsTrue() {
        Patient patient = patient();

        when(patientRepository.searchByText("john", true))
                .thenReturn(List.of(patient));

        List<PatientResponseDTO> patients = patientService.findAll("  john  ", true);

        assertEquals(1, patients.size());
        assertPatientResponse(patients.getFirst());

        verify(patientRepository).searchByText("john", true);
        verify(patientRepository, never()).findAllByArchiveFilter(anyBoolean());
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
                () -> assertEquals("MRN-00001", response.medicalRecordNumber()),
                () -> assertFalse(response.archived()),
                () -> assertNull(response.archivedAt())
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

    @Test
    void archivePatientMarksPatientAsArchived() {
        UUID patientId = UUID.randomUUID();
        Patient patient = patient();

        when(patientRepository.findById(patientId))
                .thenReturn(Optional.of(patient));

        PatientResponseDTO response = patientService.archivePatient(patientId);

        assertAll(
                () -> assertTrue(response.archived()),
                () -> assertNotNull(response.archivedAt()),
                () -> assertEquals("John", response.firstName()),
                () -> assertEquals("Doe", response.lastName()),
                () -> assertEquals("MRN-001", response.medicalRecordNumber())
        );

        verify(patientRepository).findById(patientId);
    }

    @Test
    void archivePatientThrowsWhenPatientDoesNotExist() {
        UUID patientId = UUID.randomUUID();

        when(patientRepository.findById(patientId))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> patientService.archivePatient(patientId)
        );

        assertEquals("Patient not found with id: " + patientId, exception.getMessage());

        verify(patientRepository).findById(patientId);
    }

    @Test
    void restorePatientClearsArchivedFields() {
        UUID patientId = UUID.randomUUID();
        Patient patient = patient();
        patient.archive();

        when(patientRepository.findById(patientId))
                .thenReturn(Optional.of(patient));

        PatientResponseDTO response = patientService.restorePatient(patientId);

        assertAll(
                () -> assertFalse(response.archived()),
                () -> assertNull(response.archivedAt()),
                () -> assertEquals("John", response.firstName()),
                () -> assertEquals("Doe", response.lastName()),
                () -> assertEquals("MRN-001", response.medicalRecordNumber())
        );

        verify(patientRepository).findById(patientId);
    }

    @Test
    void restorePatientThrowsWhenPatientDoesNotExist() {
        UUID patientId = UUID.randomUUID();

        when(patientRepository.findById(patientId))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> patientService.restorePatient(patientId)
        );

        assertEquals("Patient not found with id: " + patientId, exception.getMessage());

        verify(patientRepository).findById(patientId);
    }

    private static void assertPatientResponse(PatientResponseDTO patient) {
        assertAll(
                () -> assertEquals("John", patient.firstName()),
                () -> assertEquals("Doe", patient.lastName()),
                () -> assertEquals(LocalDate.parse("1990-01-15"), patient.dateOfBirth()),
                () -> assertEquals("MRN-001", patient.medicalRecordNumber()),
                () -> assertFalse(patient.archived()),
                () -> assertNull(patient.archivedAt())
        );
    }
}