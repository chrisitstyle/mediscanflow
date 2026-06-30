import { apiFetch } from "@/lib/apiClient";
import type { Patient } from "@/types/patient";

export function getPatients(): Promise<Patient[]> {
  return apiFetch<Patient[]>("/patients");
}
