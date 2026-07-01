"use client";

import Link from "next/link";
import { FileSearch } from "lucide-react";
import { useQuery } from "@tanstack/react-query";

import { getAnalyses } from "@/api/analysesApi";
import { AnalysisStatusBadge } from "@/components/status/AnalysisStatusBadge";
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
  const { data, isLoading, isFetching, isError, error } = useQuery({
    queryKey: queryKeys.analyses.list(),
    queryFn: getAnalyses,
    refetchInterval: 10_000,
  });

  const analyses = data ?? [];
  const hasAnalyses = analyses.length > 0;

  const errorMessage =
    error instanceof ApiClientError ? error.message : "Could not load analyses";

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
            <EmptyState
              icon={<FileSearch className="size-6" />}
              title="No analyses yet"
              description="Upload a scan from a patient profile to create the first AI analysis."
              className="min-h-80"
            >
              <Button asChild variant="outline">
                <Link href="/patients">Go to patients</Link>
              </Button>
            </EmptyState>
          )}

          {!isLoading && !isError && hasAnalyses && (
            <>
              <div className="grid gap-3 md:hidden">
                {analyses.map((analysis) => (
                  <div
                    key={analysis.id}
                    className="rounded-lg border bg-card p-4 text-card-foreground"
                  >
                    <div className="flex items-start justify-between gap-4">
                      <div className="min-w-0">
                        <h3 className="truncate font-medium">
                          {analysis.originalFileName}
                        </h3>
                        <p className="mt-1 text-xs text-muted-foreground">
                          {formatFileSize(analysis.fileSizeBytes)}
                        </p>
                      </div>

                      <AnalysisStatusBadge status={analysis.status} />
                    </div>

                    <div className="mt-4 grid gap-3 text-sm">
                      <MobileMetaRow
                        label="Patient"
                        value={
                          <Link
                            href={`/patients/${analysis.patientId}`}
                            className="font-medium hover:underline"
                          >
                            {analysis.patientFullName}
                          </Link>
                        }
                      />
                      <MobileMetaRow
                        label="Model"
                        value={`${analysis.modelName} / ${analysis.modelVersion}`}
                      />
                      <MobileMetaRow
                        label="Created"
                        value={formatDateTime(analysis.createdAt)}
                      />
                      <MobileMetaRow
                        label="Completed"
                        value={formatDateTime(analysis.completedAt)}
                      />
                    </div>

                    <Button
                      asChild
                      variant="outline"
                      size="sm"
                      className="mt-4 w-full"
                    >
                      <Link href={`/analyses/${analysis.id}`}>
                        View details
                      </Link>
                    </Button>
                  </div>
                ))}
              </div>

              <div className="hidden overflow-hidden rounded-lg border md:block">
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
            </>
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

type MobileMetaRowProps = {
  label: string;
  value: React.ReactNode;
};

function MobileMetaRow({ label, value }: MobileMetaRowProps) {
  return (
    <div className="flex items-start justify-between gap-4">
      <span className="text-muted-foreground">{label}</span>
      <span className="text-right font-medium">{value}</span>
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
