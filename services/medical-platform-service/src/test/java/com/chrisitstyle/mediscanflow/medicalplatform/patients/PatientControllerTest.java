package com.chrisitstyle.mediscanflow.medicalplatform.patients;

import com.chrisitstyle.mediscanflow.medicalplatform.patients.dto.PatientProfileUpdateDTO;
import com.chrisitstyle.mediscanflow.medicalplatform.patients.dto.PatientResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PatientController.class)
class PatientControllerTest {

    private static final UUID PATIENT_ID =
            UUID.fromString("9efdb5f0-733e-4f59-8a78-6240e43237c7");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientService patientService;

    @Test
    void findAllPassesSearchQueryToService() throws Exception {
        when(patientService.findAll("john"))
                .thenReturn(List.of(patientResponse()));

        mockMvc.perform(get("/api/patients?search=john").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PATIENT_ID.toString()))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[0].lastName").value("Doe"))
                .andExpect(jsonPath("$[0].medicalRecordNumber").value("MRN-0001"));

        then(patientService).should().findAll("john");
    }

    @Test
    void findAllPassesNullSearchWhenQueryIsNotProvided() throws Exception {
        when(patientService.findAll(null))
                .thenReturn(List.of(patientResponse()));

        mockMvc.perform(get("/api/patients").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PATIENT_ID.toString()))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[0].lastName").value("Doe"))
                .andExpect(jsonPath("$[0].medicalRecordNumber").value("MRN-0001"));

        then(patientService).should().findAll(null);
    }

    @Test
    void updatePatientProfileReturnsUpdatedPatient() throws Exception {
        UUID patientId = UUID.randomUUID();

        PatientResponseDTO response = patientResponse(
                patientId,
                "Anna",
                "Kowalska",
                LocalDate.of(1990, Month.APRIL, 15),
                "MRN-0001"
        );

        given(patientService.updatePatientProfile(eq(patientId), any(PatientProfileUpdateDTO.class)))
                .willReturn(response);

        mockMvc.perform(put("/api/patients/{patientId}/profile", patientId)
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Anna",
                                  "lastName": "Kowalska",
                                  "dateOfBirth": "1990-04-15"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(patientId.toString()))
                .andExpect(jsonPath("$.firstName").value("Anna"))
                .andExpect(jsonPath("$.lastName").value("Kowalska"))
                .andExpect(jsonPath("$.dateOfBirth").value("1990-04-15"))
                .andExpect(jsonPath("$.medicalRecordNumber").value("MRN-0001"));

        then(patientService).should()
                .updatePatientProfile(eq(patientId), any(PatientProfileUpdateDTO.class));
    }

    @Test
    void updatePatientProfileReturnsBadRequestWhenDateOfBirthIsInFuture() throws Exception {
        UUID patientId = UUID.randomUUID();

        mockMvc.perform(put("/api/patients/{patientId}/profile", patientId)
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Anna",
                                  "lastName": "Kowalska",
                                  "dateOfBirth": "2999-04-15"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.dateOfBirth")
                        .value("Date of birth cannot be in the future."));

        then(patientService).shouldHaveNoInteractions();
    }

    private static PatientResponseDTO patientResponse() {
        return patientResponse(
                PATIENT_ID,
                "John",
                "Doe",
                LocalDate.parse("1990-01-15"),
                "MRN-0001"
        );
    }

    private static PatientResponseDTO patientResponse(
            UUID id,
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            String medicalRecordNumber
    ) {
        return new PatientResponseDTO(
                id,
                firstName,
                lastName,
                dateOfBirth,
                medicalRecordNumber,
                Instant.parse("2026-07-01T10:00:00Z")
        );
    }
}