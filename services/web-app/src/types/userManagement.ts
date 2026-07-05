export type UserRole = "ADMIN" | "DOCTOR" | "STAFF";

export type UserStatus = "Enabled" | "Disabled";

export type User = {
  id: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  roles: UserRole[];
  status: UserStatus;
};

export type UpdateUserStatusInput = {
  status: UserStatus;
};

export type CreateUserInput = {
  firstName: string;
  lastName: string;
  email: string;
  role: UserRole;
  temporaryPassword: string;
};

export type UserCreatedResponse = {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  enabled: boolean;
};
