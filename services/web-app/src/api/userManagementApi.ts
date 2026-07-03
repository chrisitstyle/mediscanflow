import { apiFetch } from "@/lib/apiClient";
import type {
  CreateUserInput,
  UserCreatedResponse,
} from "@/types/userManagement";

export async function createUser(
  input: CreateUserInput,
): Promise<UserCreatedResponse> {
  return apiFetch<UserCreatedResponse>("/admin/users", {
    method: "POST",
    body: input,
  });
}
