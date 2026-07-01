import { apiFetch } from "@/lib/apiClient";
import type { Patient } from "@/types/patient";

export type CreatePatientInput = {
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  medicalRecordNumber: string;
};

export type GetPatientsInput = {
  search?: string;
};

export function getPatients(input: GetPatientsInput = {}): Promise<Patient[]> {
  const search = input.search?.trim();

  if (!search) {
    return apiFetch<Patient[]>("/patients");
  }

  const searchParams = new URLSearchParams({
    search,
  });

  return apiFetch<Patient[]>(`/patients?${searchParams.toString()}`);
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
