package com.chrisitstyle.mediscanflow.medicalplatform.patients;

import com.chrisitstyle.mediscanflow.medicalplatform.patients.dto.PatientResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                .andExpect(jsonPath("$[0].medicalRecordNumber").value("MRN-001"));

        verify(patientService).findAll("john");
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
                .andExpect(jsonPath("$[0].medicalRecordNumber").value("MRN-001"));

        verify(patientService).findAll(null);
    }

    private static PatientResponseDTO patientResponse() {
        return new PatientResponseDTO(
                PATIENT_ID,
                "John",
                "Doe",
                LocalDate.parse("1990-01-15"),
                "MRN-001",
                Instant.parse("2026-07-01T10:00:00Z")
        );
    }
}
