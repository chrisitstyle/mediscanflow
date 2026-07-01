"use client";

import Link from "next/link";
import { FileImage } from "lucide-react";
import { useQuery } from "@tanstack/react-query";

import { getAnalyses } from "@/api/analysesApi";
import { AnalysisStatusBadge } from "@/components/status/AnalysisStatusBadge";
import { ApiClientError } from "@/lib/apiClient";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import {
  Card,
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

function formatDateTime(value: string | null) {
  if (!value) {
    return "—";
  }

  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function formatFileSize(bytes: number) {
  if (bytes < 1024) {
    return `${bytes} B`;
  }

  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }

  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

export function AnalysisListPage() {
  const {
    data: analyses,
    isLoading,
    isFetching,
    isError,
    error,
  } = useQuery({
    queryKey: ["analyses"],
    queryFn: getAnalyses,
    refetchInterval: 10_000,
  });

  const errorMessage =
    error instanceof ApiClientError ? error.message : "Could not load analyses";

  const hasAnalyses = analyses && analyses.length > 0;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Analyses</h1>
        <p className="mt-2 text-muted-foreground">
          Browse all submitted scans and AI processing results in the system.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Analysis registry</CardTitle>
          <CardDescription>
            All analyses across registered patients, ordered by newest first.
          </CardDescription>
        </CardHeader>

        <CardContent className="space-y-4">
          {isError && (
            <Alert variant="destructive">
              <AlertTitle>Analyses unavailable</AlertTitle>
              <AlertDescription>{errorMessage}</AlertDescription>
            </Alert>
          )}

          {isLoading && <AnalysisListSkeleton />}

          {!isLoading && !isError && !hasAnalyses && (
            <div className="flex min-h-80 flex-col items-center justify-center rounded-lg border border-dashed p-10 text-center">
              <div className="flex size-12 items-center justify-center rounded-full bg-muted">
                <FileImage className="size-6 text-muted-foreground" />
              </div>

              <h2 className="mt-4 text-lg font-semibold">
                No analyses submitted yet
              </h2>

              <p className="mt-2 max-w-md text-sm text-muted-foreground">
                Create a patient and upload a scan to start AI-powered image
                analysis.
              </p>

              <Button asChild className="mt-6" variant="outline">
                <Link href="/patients">Open patients</Link>
              </Button>
            </div>
          )}

          {!isLoading && !isError && hasAnalyses && (
            <div className="overflow-hidden rounded-lg border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>File</TableHead>
                    <TableHead>Patient</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Model</TableHead>
                    <TableHead>Created</TableHead>
                    <TableHead>Completed</TableHead>
                    <TableHead className="text-right">Details</TableHead>
                  </TableRow>
                </TableHeader>

                <TableBody>
                  {analyses.map((analysis) => (
                    <TableRow key={analysis.id}>
                      <TableCell>
                        <div className="font-medium">
                          {analysis.originalFileName}
                        </div>
                        <div className="text-xs text-muted-foreground">
                          {formatFileSize(analysis.fileSizeBytes)}
                        </div>
                      </TableCell>

                      <TableCell>
                        <Link
                          href={`/patients/${analysis.patientId}`}
                          className="font-medium hover:underline"
                        >
                          {analysis.patientFullName}
                        </Link>
                      </TableCell>

                      <TableCell>
                        <AnalysisStatusBadge status={analysis.status} />
                      </TableCell>

                      <TableCell>
                        <div className="text-sm">{analysis.modelName}</div>
                        <div className="text-xs text-muted-foreground">
                          {analysis.modelVersion}
                        </div>
                      </TableCell>

                      <TableCell>
                        {formatDateTime(analysis.createdAt)}
                      </TableCell>

                      <TableCell>
                        {formatDateTime(analysis.completedAt)}
                      </TableCell>

                      <TableCell className="text-right">
                        <Button asChild variant="outline" size="sm">
                          <Link href={`/analyses/${analysis.id}`}>View</Link>
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
              Refreshing analyses...
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

function AnalysisListSkeleton() {
  return (
    <div className="space-y-3">
      <Skeleton className="h-10 w-full" />
      <Skeleton className="h-14 w-full" />
      <Skeleton className="h-14 w-full" />
      <Skeleton className="h-14 w-full" />
      <Skeleton className="h-14 w-full" />
    </div>
  );
}
