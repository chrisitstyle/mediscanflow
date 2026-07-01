import { apiFetch } from "@/lib/apiClient";
import type { Patient } from "@/types/patient";

export type CreatePatientInput = {
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  medicalRecordNumber: string;
};

export function getPatients(): Promise<Patient[]> {
  return apiFetch<Patient[]>("/patients");
}

export function getPatient(patientId: string): Promise<Patient> {
  return apiFetch<Patient>(`/patients/${patientId}`);
}

export function createPatient(input: CreatePatientInput): Promise<Patient> {
  return apiFetch<Patient>("/patients", {
    method: "POST",
    body: input,
  });
}
