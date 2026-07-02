export type UserRole = "ADMIN" | "DOCTOR" | "STAFF";

export type CurrentUser = {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: UserRole[];
};
