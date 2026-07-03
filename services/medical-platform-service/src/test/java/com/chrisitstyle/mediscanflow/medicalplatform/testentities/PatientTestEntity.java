package com.chrisitstyle.mediscanflow.medicalplatform.testentities;

import com.chrisitstyle.mediscanflow.medicalplatform.patients.Patient;

import java.time.LocalDate;

/**
 * Test factory for creating {@link Patient} instances used in unit tests.
 */
public final class PatientTestEntity {

    private PatientTestEntity() {
    }

    /**
     * Creates a default patient used by tests.
     *
     * @return patient with default test data
     */
    public static Patient patient() {
        return patient(
                "John",
                "Doe",
                LocalDate.parse("1990-01-15"),
                "MRN-001"
        );
    }

    /**
     * Creates a patient with custom test data.
     *
     * @param firstName patient first name
     * @param lastName patient last name
     * @param dateOfBirth patient date of birth
     * @param medicalRecordNumber patient medical record number
     * @return patient with provided test data
     */
    public static Patient patient(
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
}