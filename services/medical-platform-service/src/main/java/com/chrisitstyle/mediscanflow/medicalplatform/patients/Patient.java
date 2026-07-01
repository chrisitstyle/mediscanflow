package com.chrisitstyle.mediscanflow.medicalplatform.patients;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "patients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Patient {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false, unique = true)
    private String medicalRecordNumber;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean archived = false;

    @Column(name = "archived_at")
    private Instant archivedAt;

    private Patient(
            UUID id,
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            String medicalRecordNumber,
            Instant createdAt
    ) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.medicalRecordNumber = medicalRecordNumber;
        this.createdAt = createdAt;
    }

    public boolean isArchived() {
        return archived;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public void archive() {
        if (!archived) {
            archived = true;
            archivedAt = Instant.now();
        }
    }

    public void restore() {
        if (archived) {
            archived = false;
            archivedAt = null;
        }
    }

    public static Patient create(
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            String medicalRecordNumber
    ) {
        return new Patient(
                UUID.randomUUID(),
                firstName,
                lastName,
                dateOfBirth,
                medicalRecordNumber,
                Instant.now()
        );
    }

    public void updateProfile(String firstName, String lastName, LocalDate dateOfBirth) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
    }
}
