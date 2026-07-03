export type UserRole = "ADMIN" | "DOCTOR" | "STAFF";

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
