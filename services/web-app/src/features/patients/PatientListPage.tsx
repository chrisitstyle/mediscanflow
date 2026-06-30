"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { getPatients } from "@/api/patientsApi";
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
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

export function PatientListPage() {
  const {
    data: patients,
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: ["patients"],
    queryFn: getPatients,
  });

  const errorMessage =
    error instanceof ApiClientError ? error.message : "Could not load patients";

  return (
    <main className="mx-auto w-full max-w-6xl px-6 py-8">
      <Card>
        <CardHeader>
          <div>
            <Badge variant="secondary">Patients</Badge>

            <CardTitle className="mt-4 text-3xl">Patient registry</CardTitle>

            <CardDescription className="mt-2">
              Manage patients and review their medical image analyses.
            </CardDescription>
          </div>

          <CardAction>
            <Button asChild>
              <Link href="/patients/new">New patient</Link>
            </Button>
          </CardAction>
        </CardHeader>

        <CardContent>
          {isLoading && <PatientsTableSkeleton />}

          {isError && (
            <Alert variant="destructive">
              <AlertTitle>Could not load patients</AlertTitle>
              <AlertDescription>{errorMessage}</AlertDescription>
            </Alert>
          )}

          {!isLoading && !isError && patients?.length === 0 && (
            <div className="flex flex-col items-center justify-center rounded-lg border border-dashed p-10 text-center">
              <h2 className="text-lg font-semibold">No patients yet</h2>

              <p className="mt-2 max-w-md text-sm text-muted-foreground">
                Create your first patient to start uploading scans and running
                AI analysis.
              </p>

              <Button asChild className="mt-6">
                <Link href="/patients/new">Create patient</Link>
              </Button>
            </div>
          )}

          {!isLoading && !isError && patients && patients.length > 0 && (
            <div className="overflow-hidden rounded-lg border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Patient</TableHead>
                    <TableHead>Medical record number</TableHead>
                    <TableHead>Date of birth</TableHead>
                    <TableHead className="text-right">Details</TableHead>
                  </TableRow>
                </TableHeader>

                <TableBody>
                  {patients.map((patient) => (
                    <TableRow key={patient.id}>
                      <TableCell>
                        <div className="font-medium">
                          {patient.firstName} {patient.lastName}
                        </div>

                        <div className="text-xs text-muted-foreground">
                          ID: {patient.id}
                        </div>
                      </TableCell>

                      <TableCell>
                        <Badge variant="outline">
                          {patient.medicalRecordNumber}
                        </Badge>
                      </TableCell>

                      <TableCell>{patient.dateOfBirth}</TableCell>

                      <TableCell className="text-right">
                        <Button asChild variant="outline" size="sm">
                          <Link href={`/patients/${patient.id}`}>View</Link>
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>
    </main>
  );
}

function PatientsTableSkeleton() {
  return (
    <div className="flex flex-col gap-3">
      <Skeleton className="h-10 w-full" />
      <Skeleton className="h-14 w-full" />
      <Skeleton className="h-14 w-full" />
      <Skeleton className="h-14 w-full" />
    </div>
  );
}
