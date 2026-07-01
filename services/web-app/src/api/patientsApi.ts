import { apiFetch } from "@/lib/apiClient";
import type { Patient } from "@/types/patient";

export type CreatePatientInput = {
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  medicalRecordNumber: string;
};

export type PatientProfileUpdateInput = {
  firstName: string;
  lastName: string;
  dateOfBirth: string;
};

export type GetPatientsInput = {
  search?: string;
  includeArchived?: boolean;
};

export async function getPatients(
  input: GetPatientsInput = {},
): Promise<Patient[]> {
  const searchParams = new URLSearchParams();

  if (input.search) {
    searchParams.set("search", input.search);
  }

  if (input.includeArchived) {
    searchParams.set("includeArchived", "true");
  }

  const query = searchParams.toString();

  return apiFetch<Patient[]>(`/patients${query ? `?${query}` : ""}`);
}

export async function getPatient(patientId: string): Promise<Patient> {
  return apiFetch<Patient>(`/patients/${patientId}`);
}

export async function createPatient(
  input: CreatePatientInput,
): Promise<Patient> {
  return apiFetch<Patient>("/patients", {
    method: "POST",
    body: input,
  });
}

export async function updatePatientProfile(
  patientId: string,
  input: PatientProfileUpdateInput,
): Promise<Patient> {
  return apiFetch<Patient>(`/patients/${patientId}/profile`, {
    method: "PUT",
    body: input,
  });
}

export async function archivePatient(patientId: string): Promise<Patient> {
  return apiFetch<Patient>(`/patients/${patientId}/archive`, {
    method: "PATCH",
  });
}

export async function restorePatient(patientId: string): Promise<Patient> {
  return apiFetch<Patient>(`/patients/${patientId}/restore`, {
    method: "PATCH",
  });
}
