"use client";

import Link from "next/link";
import { Search, SearchX, UserPlus, UsersRound, X } from "lucide-react";
import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";

import { getPatients } from "@/api/patientsApi";
import { EmptyState } from "@/components/EmptyState";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { ApiClientError } from "@/lib/apiClient";
import { queryKeys } from "@/lib/queryKeys";

function useDebouncedValue<T>(value: T, delayMs: number): T {
  const [debouncedValue, setDebouncedValue] = useState(value);

  useEffect(() => {
    const timeoutId = setTimeout(() => {
      setDebouncedValue(value);
    }, delayMs);

    return () => {
      clearTimeout(timeoutId);
    };
  }, [value, delayMs]);

  return debouncedValue;
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
  }).format(new Date(value));
}

export function PatientListPage() {
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search, 300);
  const normalizedSearch = debouncedSearch.trim();

  const { data, isLoading, isFetching, isError, error } = useQuery({
    queryKey: queryKeys.patients.list(normalizedSearch),
    queryFn: () => getPatients({ search: normalizedSearch }),
  });

  const patients = data ?? [];
  const hasSearch = normalizedSearch.length > 0;
  const hasPatients = patients.length > 0;

  const errorMessage =
    error instanceof ApiClientError ? error.message : "Could not load patients";

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-4 md:flex-row md:items-start">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Patients</h1>
          <p className="mt-2 text-muted-foreground">
            Browse registered patients and open their scan analyses.
          </p>
        </div>

        <Button asChild>
          <Link href="/patients/new">
            <UserPlus className="size-4" />
            New patient
          </Link>
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Patient registry</CardTitle>
          <CardDescription>
            Search by first name, last name, or medical record number.
          </CardDescription>
        </CardHeader>

        <CardContent className="space-y-4">
          <div className="relative max-w-md">
            <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />

            <Input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search patients..."
              className="pl-9 pr-10"
            />

            {search.trim().length > 0 && (
              <Button
                type="button"
                variant="ghost"
                size="icon"
                className="absolute right-1 top-1/2 size-8 -translate-y-1/2"
                onClick={() => setSearch("")}
                aria-label="Clear search"
              >
                <X className="size-4" />
              </Button>
            )}
          </div>

          {isError && (
            <Alert variant="destructive">
              <AlertTitle>Patients unavailable</AlertTitle>
              <AlertDescription>{errorMessage}</AlertDescription>
            </Alert>
          )}

          {isLoading && <PatientListSkeleton />}

          {!isLoading && !isError && !hasPatients && (
            <EmptyState
              icon={
                hasSearch ? (
                  <SearchX className="size-6" />
                ) : (
                  <UsersRound className="size-6" />
                )
              }
              title={hasSearch ? "No patients found" : "No patients yet"}
              description={
                hasSearch
                  ? "No patient matches this search. Try another name or medical record number."
                  : "Create the first patient record to start uploading scans and running AI analysis."
              }
              className="min-h-80"
            >
              {hasSearch ? (
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setSearch("")}
                >
                  Clear search
                </Button>
              ) : (
                <Button asChild>
                  <Link href="/patients/new">Create patient</Link>
                </Button>
              )}
            </EmptyState>
          )}

          {!isLoading && !isError && hasPatients && (
            <div className="overflow-hidden rounded-lg border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Patient</TableHead>
                    <TableHead>Medical record number</TableHead>
                    <TableHead>Date of birth</TableHead>
                    <TableHead>Created</TableHead>
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
                      </TableCell>

                      <TableCell>{patient.medicalRecordNumber}</TableCell>

                      <TableCell>{formatDate(patient.dateOfBirth)}</TableCell>

                      <TableCell>{formatDate(patient.createdAt)}</TableCell>

                      <TableCell className="text-right">
                        <Button asChild variant="outline" size="sm">
                          <Link href={`/patients/${patient.id}`}>Open</Link>
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}

          {isFetching && !isLoading && (
            <p className="text-xs text-muted-foreground">
              Refreshing patient results...
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

function PatientListSkeleton() {
  return (
    <div className="space-y-3">
      <Skeleton className="h-10 w-full" />
      <Skeleton className="h-14 w-full" />
      <Skeleton className="h-14 w-full" />
      <Skeleton className="h-14 w-full" />
    </div>
  );
}
