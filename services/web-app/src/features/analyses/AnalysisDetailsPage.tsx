"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";

import { getAnalysis } from "@/api/analysesApi";
import { DetectionTable } from "@/features/analyses/DetectionTable";
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

function getStatusVariant(status: AnalysisStatus) {
  if (status === "COMPLETED") {
    return "default";
  }

  if (status === "FAILED") {
    return "destructive";
  }

  return "secondary";
}

function shouldPoll(status?: AnalysisStatus) {
  return (
    status === "QUEUED" || status === "PROCESSING" || status === "UPLOADED"
  );
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

export function AnalysisDetailsPage() {
  const params = useParams<{ analysisId: string }>();
  const analysisId = params.analysisId;

  const analysisQuery = useQuery({
    queryKey: ["analyses", analysisId],
    queryFn: () => getAnalysis(analysisId),
    enabled: !!analysisId,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return shouldPoll(status) ? 3000 : false;
    },
  });

  const errorMessage =
    analysisQuery.error instanceof ApiClientError
      ? analysisQuery.error.message
      : "Could not load analysis";

  if (analysisQuery.isLoading) {
    return (
      <main className="mx-auto w-full max-w-6xl px-6 py-8">
        <AnalysisDetailsSkeleton />
      </main>
    );
  }

  if (analysisQuery.isError) {
    return (
      <main className="mx-auto w-full max-w-6xl px-6 py-8">
        <Alert variant="destructive">
          <AlertTitle>Could not load analysis</AlertTitle>
          <AlertDescription>{errorMessage}</AlertDescription>
        </Alert>
      </main>
    );
  }

  const analysis = analysisQuery.data;

  if (!analysis) {
    return null;
  }

  return (
    <main className="mx-auto flex w-full max-w-6xl flex-col gap-6 px-6 py-8">
      <div className="flex items-center justify-between gap-4">
        <Button asChild variant="outline" size="sm">
          <Link href={`/patients/${analysis.patientId}`}>Back to patient</Link>
        </Button>

        <Badge variant={getStatusVariant(analysis.status)}>
          {analysis.status}
        </Badge>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-3xl">Analysis details</CardTitle>
          <CardDescription>
            Review uploaded scan, AI result image, detections and model
            metadata.
          </CardDescription>
        </CardHeader>

        <CardContent>
          <dl className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
            <div>
              <dt className="text-sm font-medium text-muted-foreground">
                Original file
              </dt>
              <dd className="mt-1 text-sm font-semibold">
                {analysis.originalFileName}
              </dd>
            </div>

            <div>
              <dt className="text-sm font-medium text-muted-foreground">
                File size
              </dt>
              <dd className="mt-1 text-sm font-semibold">
                {formatFileSize(analysis.fileSizeBytes)}
              </dd>
            </div>

            <div>
              <dt className="text-sm font-medium text-muted-foreground">
                Created
              </dt>
              <dd className="mt-1 text-sm font-semibold">
                {formatDateTime(analysis.createdAt)}
              </dd>
            </div>

            <div>
              <dt className="text-sm font-medium text-muted-foreground">
                Completed
              </dt>
              <dd className="mt-1 text-sm font-semibold">
                {formatDateTime(analysis.completedAt)}
              </dd>
            </div>

            <div>
              <dt className="text-sm font-medium text-muted-foreground">
                Model
              </dt>
              <dd className="mt-1 text-sm font-semibold">
                {analysis.modelName}
              </dd>
              <dd className="text-xs text-muted-foreground">
                {analysis.modelVersion}
              </dd>
            </div>

            <div className="sm:col-span-2 lg:col-span-3">
              <dt className="text-sm font-medium text-muted-foreground">
                Analysis ID
              </dt>
              <dd className="mt-1 break-all font-mono text-sm">
                {analysis.id}
              </dd>
            </div>
          </dl>

          {analysis.errorMessage && (
            <Alert variant="destructive" className="mt-6">
              <AlertTitle>Analysis failed</AlertTitle>
              <AlertDescription>{analysis.errorMessage}</AlertDescription>
            </Alert>
          )}
        </CardContent>
      </Card>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Original image</CardTitle>
            <CardDescription>Uploaded scan image.</CardDescription>
          </CardHeader>

          <CardContent>
            {analysis.originalImageUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={analysis.originalImageUrl}
                alt="Original scan"
                className="max-h-130 w-full rounded-lg border object-contain"
              />
            ) : (
              <ImagePlaceholder message="Original image URL is not available." />
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Result image</CardTitle>
            <CardDescription>
              Annotated image generated by the AI worker.
            </CardDescription>
          </CardHeader>

          <CardContent>
            {analysis.resultImageUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={analysis.resultImageUrl}
                alt="Annotated result scan"
                className="max-h-130 w-full rounded-lg border object-contain"
              />
            ) : (
              <ImagePlaceholder message="Result image is not available yet." />
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Detections</CardTitle>
          <CardDescription>
            Bounding boxes returned by the AI inference worker.
          </CardDescription>
        </CardHeader>

        <CardContent>
          <DetectionTable detections={analysis.detections} />
        </CardContent>
      </Card>
    </main>
  );
}

function ImagePlaceholder({ message }: { message: string }) {
  return (
    <div className="flex min-h-80 items-center justify-center rounded-lg border border-dashed p-8 text-center text-sm text-muted-foreground">
      {message}
    </div>
  );
}

function AnalysisDetailsSkeleton() {
  return (
    <div className="flex flex-col gap-6">
      <Skeleton className="h-9 w-40" />

      <Card>
        <CardHeader>
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

      <div className="grid gap-6 lg:grid-cols-2">
        <Skeleton className="h-96 w-full" />
        <Skeleton className="h-96 w-full" />
      </div>
    </div>
  );
}
