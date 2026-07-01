"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";

import { getPatient } from "@/api/patientsApi";
import { getPatientAnalyses } from "@/api/analysesApi";
import { PatientAnalysesList } from "@/features/analyses/PatientAnalysesList";
import { ApiClientError } from "@/lib/apiClient";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

function formatDate(value: string) {
  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
  }).format(new Date(value));
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export function PatientDetailsPage() {
  const params = useParams<{ patientId: string }>();
  const patientId = params.patientId;

  const patientQuery = useQuery({
    queryKey: ["patients", patientId],
    queryFn: () => getPatient(patientId),
    enabled: !!patientId,
  });

  const analysesQuery = useQuery({
    queryKey: ["patients", patientId, "analyses"],
    queryFn: () => getPatientAnalyses(patientId),
    enabled: !!patientId,
  });

  const patientErrorMessage =
    patientQuery.error instanceof ApiClientError
      ? patientQuery.error.message
      : "Could not load patient";

  const analysesErrorMessage =
    analysesQuery.error instanceof ApiClientError
      ? analysesQuery.error.message
      : "Could not load analyses";

  if (patientQuery.isLoading) {
    return (
      <main className="mx-auto w-full max-w-6xl px-6 py-8">
        <PatientDetailsSkeleton />
      </main>
    );
  }

  if (patientQuery.isError) {
    return (
      <main className="mx-auto w-full max-w-6xl px-6 py-8">
        <Alert variant="destructive">
          <AlertTitle>Could not load patient</AlertTitle>
          <AlertDescription>{patientErrorMessage}</AlertDescription>
        </Alert>
      </main>
    );
  }

  const patient = patientQuery.data;

  if (!patient) {
    return null;
  }

  return (
    <main className="mx-auto flex w-full max-w-6xl flex-col gap-6 px-6 py-8">
      <div className="flex items-center justify-between gap-4">
        <Button asChild variant="outline" size="sm">
          <Link href="/patients">Back to patients</Link>
        </Button>

        <Button disabled>Upload scan</Button>
      </div>

      <Card>
        <CardHeader>
          <div>
            <Badge variant="secondary">Patient</Badge>

            <CardTitle className="mt-4 text-3xl">
              {patient.firstName} {patient.lastName}
            </CardTitle>

            <CardDescription className="mt-2">
              Patient details and medical image analysis history.
            </CardDescription>
          </div>

          <CardAction>
            <Badge variant="outline">{patient.medicalRecordNumber}</Badge>
          </CardAction>
        </CardHeader>

        <CardContent>
          <dl className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
            <div>
              <dt className="text-sm font-medium text-muted-foreground">
                First name
              </dt>
              <dd className="mt-1 text-sm font-semibold">
                {patient.firstName}
              </dd>
            </div>

            <div>
              <dt className="text-sm font-medium text-muted-foreground">
                Last name
              </dt>
              <dd className="mt-1 text-sm font-semibold">{patient.lastName}</dd>
            </div>

            <div>
              <dt className="text-sm font-medium text-muted-foreground">
                Date of birth
              </dt>
              <dd className="mt-1 text-sm font-semibold">
                {formatDate(patient.dateOfBirth)}
              </dd>
            </div>

            <div>
              <dt className="text-sm font-medium text-muted-foreground">
                Created at
              </dt>
              <dd className="mt-1 text-sm font-semibold">
                {formatDateTime(patient.createdAt)}
              </dd>
            </div>

            <div className="sm:col-span-2 lg:col-span-4">
              <dt className="text-sm font-medium text-muted-foreground">
                Patient ID
              </dt>
              <dd className="mt-1 break-all font-mono text-sm">{patient.id}</dd>
            </div>
          </dl>
        </CardContent>
      </Card>

      {analysesQuery.isLoading && (
        <Card>
          <CardHeader>
            <CardTitle>Analyses</CardTitle>
            <CardDescription>Loading patient analyses...</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex flex-col gap-3">
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-14 w-full" />
              <Skeleton className="h-14 w-full" />
            </div>
          </CardContent>
        </Card>
      )}

      {analysesQuery.isError && (
        <Alert variant="destructive">
          <AlertTitle>Could not load analyses</AlertTitle>
          <AlertDescription>{analysesErrorMessage}</AlertDescription>
        </Alert>
      )}

      {analysesQuery.data && (
        <PatientAnalysesList analyses={analysesQuery.data} />
      )}
    </main>
  );
}

function PatientDetailsSkeleton() {
  return (
    <div className="flex flex-col gap-6">
      <Skeleton className="h-9 w-36" />

      <Card>
        <CardHeader>
          <Skeleton className="h-6 w-24" />
          <Skeleton className="h-10 w-80" />
          <Skeleton className="h-5 w-96" />
        </CardHeader>

        <CardContent>
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
            <Skeleton className="h-16 w-full" />
            <Skeleton className="h-16 w-full" />
            <Skeleton className="h-16 w-full" />
            <Skeleton className="h-16 w-full" />
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
