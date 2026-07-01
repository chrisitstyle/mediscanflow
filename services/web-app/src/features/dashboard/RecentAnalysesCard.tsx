"use client";

import Link from "next/link";
import { FileImage } from "lucide-react";
import { useQuery } from "@tanstack/react-query";

import { getRecentAnalyses } from "@/api/analysesApi";
import { ApiClientError } from "@/lib/apiClient";
import type { AnalysisStatus } from "@/types/analysis";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
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

function getStatusVariant(
  status: AnalysisStatus,
): "default" | "secondary" | "destructive" | "outline" {
  if (status === "COMPLETED") {
    return "default";
  }

  if (status === "FAILED") {
    return "destructive";
  }

  return "secondary";
}

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

export function RecentAnalysesCard() {
  const {
    data: analyses,
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: ["dashboard", "recent-analyses"],
    queryFn: () => getRecentAnalyses(10),
    refetchInterval: 10_000,
  });

  const errorMessage =
    error instanceof ApiClientError
      ? error.message
      : "Could not load recent analyses";

  return (
    <Card>
      <CardHeader>
        <div className="flex items-start justify-between gap-4">
          <div>
            <CardTitle>Recent analyses</CardTitle>
            <CardDescription>
              Latest submitted scans and AI processing results.
            </CardDescription>
          </div>

          <Badge variant="outline">Latest 10</Badge>
        </div>
      </CardHeader>

      <CardContent>
        {isLoading && <RecentAnalysesSkeleton />}

        {isError && (
          <Alert variant="destructive">
            <AlertTitle>Could not load recent analyses</AlertTitle>
            <AlertDescription>{errorMessage}</AlertDescription>
          </Alert>
        )}

        {!isLoading && !isError && analyses?.length === 0 && (
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

        {!isLoading && !isError && analyses && analyses.length > 0 && (
          <div className="overflow-hidden rounded-lg border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>File</TableHead>
                  <TableHead>Patient</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Model</TableHead>
                  <TableHead>Created</TableHead>
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
                      <Badge variant={getStatusVariant(analysis.status)}>
                        {analysis.status}
                      </Badge>
                    </TableCell>

                    <TableCell>
                      <div className="text-sm">{analysis.modelName}</div>
                      <div className="text-xs text-muted-foreground">
                        {analysis.modelVersion}
                      </div>
                    </TableCell>

                    <TableCell>{formatDateTime(analysis.createdAt)}</TableCell>

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
      </CardContent>
    </Card>
  );
}

function RecentAnalysesSkeleton() {
  return (
    <div className="space-y-3">
      <Skeleton className="h-10 w-full" />
      <Skeleton className="h-14 w-full" />
      <Skeleton className="h-14 w-full" />
      <Skeleton className="h-14 w-full" />
    </div>
  );
}
