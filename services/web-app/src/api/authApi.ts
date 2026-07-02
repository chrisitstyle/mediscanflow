import { apiFetch } from "@/lib/apiClient";
import type { CurrentUser } from "@/types/auth";

export function getCurrentUser(): Promise<CurrentUser> {
  return apiFetch<CurrentUser>("/auth/me");
}
