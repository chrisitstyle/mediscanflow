"use client";

import type React from "react";
import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";

import { createPatient, type CreatePatientInput } from "@/api/patientsApi";
import { ApiClientError } from "@/lib/apiClient";
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

type FormState = CreatePatientInput;
type FormErrors = Partial<Record<keyof FormState, string>>;

const emptyForm: FormState = {
  firstName: "",
  lastName: "",
  dateOfBirth: "",
  medicalRecordNumber: "",
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

  if (!values.medicalRecordNumber.trim()) {
    errors.medicalRecordNumber = "Medical record number is required.";
  }

  return errors;
}

function getTodayDateValue() {
  return new Date().toISOString().split("T")[0];
}

export function CreatePatientForm() {
  const router = useRouter();
  const queryClient = useQueryClient();

  const [values, setValues] = useState<FormState>(emptyForm);
  const [errors, setErrors] = useState<FormErrors>({});

  const mutation = useMutation({
    mutationFn: createPatient,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["patients"] });
      router.push("/patients");
    },
    onError: (error) => {
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
        ? "Could not create patient. Please try again."
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
      medicalRecordNumber: values.medicalRecordNumber.trim(),
    };

    const nextErrors = validate(trimmed);

    setErrors(nextErrors);

    if (Object.keys(nextErrors).length > 0) {
      return;
    }

    mutation.mutate(trimmed);
  }

  return (
    <main className="mx-auto w-full max-w-2xl px-6 py-8">
      <Card>
        <CardHeader>
          <Badge variant="secondary" className="w-fit">
            New patient
          </Badge>

          <CardTitle className="mt-4 text-3xl">Create patient</CardTitle>

          <CardDescription className="mt-2">
            Add a new patient record to start uploading scans and running AI
            analysis.
          </CardDescription>
        </CardHeader>

        <form onSubmit={handleSubmit} noValidate>
          <CardContent>
            {submitError && (
              <Alert variant="destructive" className="mb-6">
                <AlertTitle>Could not create patient</AlertTitle>
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

              <Field data-invalid={!!errors.medicalRecordNumber}>
                <FieldLabel htmlFor="medicalRecordNumber">
                  Medical record number
                </FieldLabel>
                <Input
                  id="medicalRecordNumber"
                  value={values.medicalRecordNumber}
                  onChange={handleChange("medicalRecordNumber")}
                  placeholder="MRN-00001"
                  aria-invalid={!!errors.medicalRecordNumber}
                  disabled={mutation.isPending}
                />
                <FieldDescription>
                  Unique identifier used across the hospital system.
                </FieldDescription>
                {errors.medicalRecordNumber && (
                  <FieldError>{errors.medicalRecordNumber}</FieldError>
                )}
              </Field>
            </FieldGroup>
          </CardContent>

          <CardFooter className="mt-6 justify-end gap-3">
            <Button asChild type="button" variant="outline">
              <Link href="/patients">Cancel</Link>
            </Button>

            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? "Creating..." : "Create patient"}
            </Button>
          </CardFooter>
        </form>
      </Card>
    </main>
  );
}
