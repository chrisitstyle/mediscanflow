import { apiFetch } from "@/lib/apiClient";
import type { SystemStatusResponse } from "@/types/systemStatus";

export function getSystemStatus(): Promise<SystemStatusResponse> {
  return apiFetch<SystemStatusResponse>("/system/status");
}
