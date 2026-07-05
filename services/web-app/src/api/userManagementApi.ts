import { apiFetch } from "@/lib/apiClient";

import type {
  CreateUserInput,
  UpdateUserStatusInput,
  User,
  UserCreatedResponse,
} from "@/types/userManagement";

export function getUsers(): Promise<User[]> {
  return apiFetch<User[]>("/admin/users");
}

export function getUser(userId: string): Promise<User> {
  return apiFetch<User>(`/admin/users/${userId}`);
}

export function updateUserStatus(
  userId: string,
  input: UpdateUserStatusInput,
): Promise<User> {
  return apiFetch<User>(`/admin/users/${userId}/status`, {
    method: "PATCH",
    body: input,
  });
}

export function createUser(
  input: CreateUserInput,
): Promise<UserCreatedResponse> {
  return apiFetch<UserCreatedResponse>("/admin/users", {
    method: "POST",
    body: input,
  });
}
