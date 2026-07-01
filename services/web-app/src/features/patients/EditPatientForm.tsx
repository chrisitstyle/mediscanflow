"use client";

import type React from "react";
import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import {
  getPatient,
  updatePatientProfile,
  type PatientProfileUpdateInput,
} from "@/api/patientsApi";
import { ApiClientError } from "@/lib/apiClient";
import { queryKeys } from "@/lib/queryKeys";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
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

type FormState = PatientProfileUpdateInput;
type FormErrors = Partial<Record<keyof FormState, string>>;

const emptyForm: FormState = {
  firstName: "",
  lastName: "",
  dateOfBirth: "",
};

function validate(values: FormState): FormErrors {
  const errors: FormErrors = {};

  if (!values.firstName.trim()) {
    errors.firstName = "First name is required.";
  }

  if (!values.lastName.trim()) {
    errors.lastName = "Last name is required.";
  }

  if (!values.dateOfBirth) {
    errors.dateOfBirth = "Date of birth is required.";
  } else if (new Date(values.dateOfBirth) > new Date()) {
    errors.dateOfBirth = "Date of birth cannot be in the future.";
  }

  return errors;
}

function getTodayDateValue() {
  return new Date().toISOString().split("T")[0];
}

export function EditPatientForm() {
  const params = useParams<{ patientId: string }>();
  const patientId = params.patientId;

  const router = useRouter();
  const queryClient = useQueryClient();

  const [values, setValues] = useState<FormState>(emptyForm);
  const [errors, setErrors] = useState<FormErrors>({});

  const patientQuery = useQuery({
    queryKey: queryKeys.patients.detail(patientId),
    queryFn: () => getPatient(patientId),
    enabled: !!patientId,
  });

  useEffect(() => {
    if (!patientQuery.data) {
      return;
    }

    setValues({
      firstName: patientQuery.data.firstName,
      lastName: patientQuery.data.lastName,
      dateOfBirth: patientQuery.data.dateOfBirth,
    });
  }, [patientQuery.data]);

  const mutation = useMutation({
    mutationFn: (input: PatientProfileUpdateInput) =>
      updatePatientProfile(patientId, input),
    onSuccess: async (patient) => {
      toast.success("Patient updated", {
        description: `${patient.firstName} ${patient.lastName} profile was updated.`,
      });

      queryClient.setQueryData(queryKeys.patients.detail(patient.id), patient);

      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: queryKeys.patients.all,
        }),
        queryClient.invalidateQueries({
          queryKey: queryKeys.patients.detail(patient.id),
        }),
      ]);

      router.push(`/patients/${patient.id}`);
    },
    onError: (error) => {
      toast.error("Could not update patient", {
        description:
          error instanceof ApiClientError
            ? error.message
            : "Unexpected error while updating patient.",
      });

      if (error instanceof ApiClientError) {
        setErrors((previousErrors) => ({
          ...previousErrors,
          ...error.validationErrors,
        }));
      }
    },
  });

  const loadErrorMessage =
    patientQuery.error instanceof ApiClientError
      ? patientQuery.error.message
      : "Could not load patient";

  const submitError =
    mutation.error instanceof ApiClientError
      ? mutation.error.message
      : mutation.isError
        ? "Could not update patient. Please try again."
        : null;

  function handleChange(field: keyof FormState) {
    return (event: React.ChangeEvent<HTMLInputElement>) => {
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

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const trimmed: FormState = {
      firstName: values.firstName.trim(),
      lastName: values.lastName.trim(),
      dateOfBirth: values.dateOfBirth,
    };

    const nextErrors = validate(trimmed);
    setErrors(nextErrors);

    if (Object.keys(nextErrors).length > 0) {
      return;
    }

    mutation.mutate(trimmed);
  }

  if (patientQuery.isLoading) {
    return <EditPatientSkeleton />;
  }

  if (patientQuery.isError) {
    return (
      <Alert variant="destructive">
        <AlertTitle>Could not load patient</AlertTitle>
        <AlertDescription>{loadErrorMessage}</AlertDescription>
      </Alert>
    );
  }

  if (!patientQuery.data) {
    return (
      <Alert variant="destructive">
        <AlertTitle>Patient not found</AlertTitle>
        <AlertDescription>
          The requested patient could not be loaded.
        </AlertDescription>
      </Alert>
    );
  }

  const patient = patientQuery.data;

  return (
    <main className="mx-auto w-full max-w-2xl px-6 py-8">
      <Card>
        <CardHeader>
          <Badge variant="secondary" className="w-fit">
            Edit patient
          </Badge>

          <CardTitle className="mt-4 text-3xl">
            Update patient profile
          </CardTitle>

          <CardDescription className="mt-2">
            Update basic profile information. The medical record number remains
            unchanged.
          </CardDescription>
        </CardHeader>

        <form onSubmit={handleSubmit} noValidate>
          <CardContent>
            {submitError && (
              <Alert variant="destructive" className="mb-6">
                <AlertTitle>Could not update patient</AlertTitle>
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

              <Field data-invalid={!!errors.dateOfBirth}>
                <FieldLabel htmlFor="dateOfBirth">Date of birth</FieldLabel>
                <Input
                  id="dateOfBirth"
                  type="date"
                  value={values.dateOfBirth}
                  onChange={handleChange("dateOfBirth")}
                  max={getTodayDateValue()}
                  aria-invalid={!!errors.dateOfBirth}
                  disabled={mutation.isPending}
                />
                {errors.dateOfBirth && (
                  <FieldError>{errors.dateOfBirth}</FieldError>
                )}
              </Field>

              <Field>
                <FieldLabel>Medical record number</FieldLabel>
                <Input value={patient.medicalRecordNumber} disabled readOnly />
                <FieldDescription>
                  Medical record number cannot be edited from this form.
                </FieldDescription>
              </Field>
            </FieldGroup>
          </CardContent>

          <CardFooter className="mt-6 justify-end gap-3">
            <Button asChild type="button" variant="outline">
              <Link href={`/patients/${patient.id}`}>Cancel</Link>
            </Button>

            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? "Saving..." : "Save changes"}
            </Button>
          </CardFooter>
        </form>
      </Card>
    </main>
  );
}

function EditPatientSkeleton() {
  return (
    <main className="mx-auto w-full max-w-2xl px-6 py-8">
      <Card>
        <CardHeader>
          <Skeleton className="h-6 w-28" />
          <Skeleton className="mt-4 h-10 w-80" />
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
