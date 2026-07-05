"use client";

import { useState } from "react";
import type { ChangeEvent, SubmitEventHandler } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { UserPlus } from "lucide-react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { createUser } from "@/api/userManagementApi";
import { AccessDenied } from "@/components/AccessDenied";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { useCurrentUser } from "@/hooks/useCurrentUser";
import { ApiClientError } from "@/lib/apiClient";
import { canManageUsers } from "@/lib/permissions";
import { queryKeys } from "@/lib/queryKeys";
import { cn } from "@/lib/utils";
import type { CreateUserInput, UserRole } from "@/types/userManagement";

type FormState = CreateUserInput;
type FormErrors = Partial<Record<keyof FormState, string>>;

const ROLE_OPTIONS: Array<{
  value: UserRole;
  label: string;
  description: string;
}> = [
  {
    value: "ADMIN",
    label: "Admin",
    description: "Full system access, including user management.",
  },
  {
    value: "DOCTOR",
    label: "Doctor",
    description: "Can manage patients, upload scans and retry analyses.",
  },
  {
    value: "STAFF",
    label: "Staff",
    description: "Read-only access to patients, analyses and activity.",
  },
];

const INITIAL_VALUES: FormState = {
  firstName: "",
  lastName: "",
  email: "",
  role: "DOCTOR",
  temporaryPassword: "",
};

function validate(values: FormState): FormErrors {
  const errors: FormErrors = {};

  if (!values.firstName.trim()) {
    errors.firstName = "First name is required.";
  }

  if (!values.lastName.trim()) {
    errors.lastName = "Last name is required.";
  }

  if (!values.email.trim()) {
    errors.email = "Email is required.";
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email.trim())) {
    errors.email = "Email must be valid.";
  }

  if (!values.role) {
    errors.role = "Role is required.";
  }

  if (!values.temporaryPassword) {
    errors.temporaryPassword = "Temporary password is required.";
  } else if (values.temporaryPassword.length < 8) {
    errors.temporaryPassword =
      "Temporary password must be at least 8 characters.";
  }

  return errors;
}

export function CreateUserForm() {
  const currentUserQuery = useCurrentUser();
  const currentUser = currentUserQuery.data;

  if (currentUserQuery.isLoading) {
    return <CreateUserSkeleton />;
  }

  if (!canManageUsers(currentUser)) {
    return (
      <AccessDenied
        title="User management restricted"
        description="Only admins can create new MediScanFlow users."
        backHref="/"
        backLabel="Back to dashboard"
      />
    );
  }

  return <CreateUserFormContent />;
}

function CreateUserFormContent() {
  const router = useRouter();
  const queryClient = useQueryClient();

  const [values, setValues] = useState<FormState>(INITIAL_VALUES);
  const [errors, setErrors] = useState<FormErrors>({});
  const [redirectToDashboard, setRedirectToDashboard] = useState(true);

  const mutation = useMutation({
    mutationFn: createUser,
    onSuccess: async (createdUser) => {
      toast.success("User created", {
        description: `${createdUser.email} was created as ${createdUser.role}.`,
      });

      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: queryKeys.audit.recent(10),
        }),
        queryClient.invalidateQueries({ queryKey: ["audit"] }),
        queryClient.invalidateQueries({
          queryKey: queryKeys.users.all,
        }),
      ]);

      if (redirectToDashboard) {
        router.push("/");
        return;
      }

      setValues({ ...INITIAL_VALUES });
      setErrors({});
    },
    onError: (error) => {
      toast.error("Could not create user", {
        description:
          error instanceof ApiClientError
            ? error.message
            : "Unexpected error while creating user.",
      });

      if (error instanceof ApiClientError) {
        setErrors((previousErrors) => ({
          ...previousErrors,
          ...error.validationErrors,
        }));
      }
    },
  });

  const submitError =
    mutation.error instanceof ApiClientError
      ? mutation.error.message
      : mutation.isError
        ? "Could not create user. Please try again."
        : null;

  function handleChange(field: keyof FormState) {
    return (event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
      const value = event.target.value;

      setValues((previousValues) => ({
        ...previousValues,
        [field]: value,
      }));

      setErrors((previousErrors) => ({
        ...previousErrors,
        [field]: undefined,
      }));

      mutation.reset();
    };
  }

  const handleSubmit: SubmitEventHandler<HTMLFormElement> = (event) => {
    event.preventDefault();

    const trimmed: FormState = {
      firstName: values.firstName.trim(),
      lastName: values.lastName.trim(),
      email: values.email.trim().toLowerCase(),
      role: values.role,
      temporaryPassword: values.temporaryPassword,
    };

    const nextErrors = validate(trimmed);
    setErrors(nextErrors);

    if (Object.keys(nextErrors).length > 0) {
      return;
    }

    mutation.mutate(trimmed);
  };

  return (
    <main className="mx-auto w-full max-w-2xl px-6 py-8">
      <Card>
        <CardHeader>
          <div className="flex flex-col gap-5 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <Badge variant="secondary" className="w-fit">
                Admin
              </Badge>

              <CardTitle className="mt-4 flex items-center gap-2 text-3xl">
                <UserPlus className="size-7" />
                Create user
              </CardTitle>
            </div>

            <div className="flex shrink-0 items-center justify-end gap-3 rounded-full border bg-muted/40 px-3 py-2">
              <span
                id="redirect-to-dashboard-label"
                className="text-sm font-medium text-muted-foreground"
              >
                Go to dashboard
              </span>

              <button
                type="button"
                role="switch"
                aria-checked={redirectToDashboard}
                aria-labelledby="redirect-to-dashboard-label"
                disabled={mutation.isPending}
                onClick={() =>
                  setRedirectToDashboard((previousValue) => !previousValue)
                }
                className={cn(
                  "relative inline-flex h-6 w-11 shrink-0 items-center rounded-full transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50",
                  redirectToDashboard ? "bg-primary" : "bg-muted-foreground/30",
                )}
              >
                <span
                  className={cn(
                    "pointer-events-none block size-5 rounded-full bg-background shadow-sm transition-transform",
                    redirectToDashboard ? "translate-x-5.5" : "translate-x-0.5",
                  )}
                />
              </button>
            </div>
          </div>
        </CardHeader>

        <form onSubmit={handleSubmit} noValidate>
          <CardContent>
            {submitError && (
              <Alert variant="destructive" className="mb-6">
                <AlertTitle>Could not create user</AlertTitle>
                <AlertDescription>{submitError}</AlertDescription>
              </Alert>
            )}

            <FieldGroup>
              <Field data-invalid={!!errors.firstName}>
                <FieldLabel htmlFor="firstName">First name</FieldLabel>
                <Input
                  id="firstName"
                  value={values.firstName}
                  onChange={handleChange("firstName")}
                  autoComplete="given-name"
                  aria-invalid={!!errors.firstName}
                  disabled={mutation.isPending}
                />
                {errors.firstName && (
                  <FieldError>{errors.firstName}</FieldError>
                )}
              </Field>

              <Field data-invalid={!!errors.lastName}>
                <FieldLabel htmlFor="lastName">Last name</FieldLabel>
                <Input
                  id="lastName"
                  value={values.lastName}
                  onChange={handleChange("lastName")}
                  autoComplete="family-name"
                  aria-invalid={!!errors.lastName}
                  disabled={mutation.isPending}
                />
                {errors.lastName && <FieldError>{errors.lastName}</FieldError>}
              </Field>

              <Field data-invalid={!!errors.email}>
                <FieldLabel htmlFor="email">Email</FieldLabel>
                <Input
                  id="email"
                  type="email"
                  value={values.email}
                  onChange={handleChange("email")}
                  autoComplete="email"
                  aria-invalid={!!errors.email}
                  disabled={mutation.isPending}
                />
                {errors.email && <FieldError>{errors.email}</FieldError>}
              </Field>

              <Field data-invalid={!!errors.role}>
                <FieldLabel htmlFor="role">Role</FieldLabel>

                <select
                  id="role"
                  value={values.role}
                  onChange={handleChange("role")}
                  aria-invalid={!!errors.role}
                  disabled={mutation.isPending}
                  className="h-10 rounded-md border border-input bg-background px-3 py-2 text-sm shadow-xs outline-none transition-colors focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {ROLE_OPTIONS.map((role) => (
                    <option key={role.value} value={role.value}>
                      {role.label}
                    </option>
                  ))}
                </select>

                <FieldDescription>
                  {
                    ROLE_OPTIONS.find((role) => role.value === values.role)
                      ?.description
                  }
                </FieldDescription>

                {errors.role && <FieldError>{errors.role}</FieldError>}
              </Field>

              <Field data-invalid={!!errors.temporaryPassword}>
                <FieldLabel htmlFor="temporaryPassword">
                  Temporary password
                </FieldLabel>
                <Input
                  id="temporaryPassword"
                  type="password"
                  value={values.temporaryPassword}
                  onChange={handleChange("temporaryPassword")}
                  autoComplete="new-password"
                  aria-invalid={!!errors.temporaryPassword}
                  disabled={mutation.isPending}
                />
                <FieldDescription>
                  The user will be required to change this password after first
                  login.
                </FieldDescription>
                {errors.temporaryPassword && (
                  <FieldError>{errors.temporaryPassword}</FieldError>
                )}
              </Field>
            </FieldGroup>
          </CardContent>

          <CardFooter className="mt-6 justify-end gap-3">
            <Button asChild type="button" variant="outline">
              <Link href="/">Cancel</Link>
            </Button>

            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? "Creating..." : "Create user"}
            </Button>
          </CardFooter>
        </form>
      </Card>
    </main>
  );
}

function CreateUserSkeleton() {
  return (
    <main className="mx-auto w-full max-w-2xl px-6 py-8">
      <Card>
        <CardHeader>
          <Skeleton className="h-6 w-20" />
          <Skeleton className="mt-4 h-10 w-72" />
          <Skeleton className="mt-2 h-5 w-96" />
        </CardHeader>

        <CardContent className="space-y-6">
          <Skeleton className="h-20 w-full" />
          <Skeleton className="h-20 w-full" />
          <Skeleton className="h-20 w-full" />
          <Skeleton className="h-20 w-full" />
        </CardContent>

        <CardFooter className="mt-6 justify-end gap-3">
          <Skeleton className="h-10 w-24" />
          <Skeleton className="h-10 w-32" />
        </CardFooter>
      </Card>
    </main>
  );
}
