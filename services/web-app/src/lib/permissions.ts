import type { CurrentUser, UserRole } from "@/types/auth";

export function canManageUsers(user: CurrentUser | null | undefined) {
  return user?.roles.includes("ADMIN") ?? false;
}

export function hasRole(user: CurrentUser | undefined, role: UserRole) {
  return user?.roles.includes(role) ?? false;
}

export function hasAnyRole(user: CurrentUser | undefined, roles: UserRole[]) {
  return roles.some((role) => hasRole(user, role));
}

export function canWriteMedicalData(user: CurrentUser | undefined) {
  return hasAnyRole(user, ["ADMIN", "DOCTOR"]);
}

export function canViewSystemStatus(user: CurrentUser | undefined) {
  return hasRole(user, "ADMIN");
}
