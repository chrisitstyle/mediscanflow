package com.chrisitstyle.mediscanflow.medicalplatform.patients.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record PatientProfileUpdateDTO(
        @NotBlank(message = "First name is required.")
        String firstName,

        @NotBlank(message = "Last name is required.")
        String lastName,

        @NotNull(message = "Date of birth is required.")
        @PastOrPresent(message = "Date of birth cannot be in the future.")
        LocalDate dateOfBirth
) {
}
