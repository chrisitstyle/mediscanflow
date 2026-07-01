"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { FileImage, RotateCcw } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { getAnalysis, retryAnalysis } from "@/api/analysesApi";
import { AnalysisStatusBadge } from "@/components/status/AnalysisStatusBadge";
import { ApiClientError } from "@/lib/apiClient";

import { DetectionTable } from "@/features/analyses/DetectionTable";

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

const POLLING_STATUSES = ["UPLOADED", "QUEUED", "PROCESSING"];

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

  const queryClient = useQueryClient();

  const {
    data: analysis,
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: ["analysis", analysisId],
    queryFn: () => getAnalysis(analysisId),
    refetchInterval: (query) => {
      const currentAnalysis = query.state.data;

      if (
        currentAnalysis &&
        POLLING_STATUSES.includes(currentAnalysis.status)
      ) {
        return 3_000;
      }

      return false;
    },
  });

  const retryMutation = useMutation({
    mutationFn: retryAnalysis,
    onSuccess: (retriedAnalysis) => {
      queryClient.setQueryData(
        ["analysis", retriedAnalysis.id],
        retriedAnalysis,
      );

      void queryClient.invalidateQueries({
        queryKey: ["analysis", retriedAnalysis.id],
      });

      void queryClient.invalidateQueries({
        queryKey: ["patient-analyses", retriedAnalysis.patientId],
      });

      void queryClient.invalidateQueries({
        queryKey: ["dashboard", "summary"],
      });

      void queryClient.invalidateQueries({
        queryKey: ["dashboard", "recent-analyses"],
      });
    },
  });

  const errorMessage =
    error instanceof ApiClientError ? error.message : "Could not load analysis";

  const retryErrorMessage =
    retryMutation.error instanceof ApiClientError
      ? retryMutation.error.message
      : "Could not retry this analysis";

  if (isLoading) {
    return <AnalysisDetailsSkeleton />;
  }

  if (isError) {
    return (
      <Alert variant="destructive">
        <AlertTitle>Analysis not available</AlertTitle>
        <AlertDescription>{errorMessage}</AlertDescription>
      </Alert>
    );
  }

  if (!analysis) {
    return (
      <Alert variant="destructive">
        <AlertTitle>Analysis not found</AlertTitle>
        <AlertDescription>
          The requested analysis could not be loaded.
        </AlertDescription>
      </Alert>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-4 md:flex-row md:items-start">
        <div>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-3xl font-bold tracking-tight">
              Analysis details
            </h1>

            <AnalysisStatusBadge status={analysis.status} />
          </div>

          <p className="mt-2 text-muted-foreground">
            AI scan analysis for{" "}
            <span className="font-medium">{analysis.originalFileName}</span>.
          </p>
        </div>

        <div className="flex flex-wrap gap-2">
          <Button asChild variant="outline">
            <Link href={`/patients/${analysis.patientId}`}>
              Back to patient
            </Link>
          </Button>

          {analysis.status === "FAILED" && (
            <Button
              type="button"
              onClick={() => retryMutation.mutate(analysis.id)}
              disabled={retryMutation.isPending}
            >
              <RotateCcw
                className={
                  retryMutation.isPending ? "size-4 animate-spin" : "size-4"
                }
              />
              {retryMutation.isPending ? "Retrying..." : "Retry analysis"}
            </Button>
          )}
        </div>
      </div>

      {analysis.status === "FAILED" && (
        <Alert variant="destructive">
          <AlertTitle>AI processing failed</AlertTitle>
          <AlertDescription>
            {analysis.errorMessage ??
              "The AI worker could not complete this analysis."}
          </AlertDescription>
        </Alert>
      )}

      {retryMutation.isError && (
        <Alert variant="destructive">
          <AlertTitle>Retry failed</AlertTitle>
          <AlertDescription>{retryErrorMessage}</AlertDescription>
        </Alert>
      )}

      <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Images</CardTitle>
              <CardDescription>
                Original scan and AI-generated result image.
              </CardDescription>
            </CardHeader>

            <CardContent>
              <div className="grid gap-4 md:grid-cols-2">
                <ImagePreview
                  title="Original scan"
                  imageUrl={analysis.originalImageUrl}
                  fallback="Original image is not available."
                />

                <ImagePreview
                  title="AI result"
                  imageUrl={analysis.resultImageUrl}
                  fallback={
                    analysis.status === "COMPLETED"
                      ? "Result image is not available."
                      : "Result image will appear after processing."
                  }
                />
              </div>
            </CardContent>
          </Card>

          <DetectionTable detections={analysis.detections} />
        </div>

        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Metadata</CardTitle>
              <CardDescription>Analysis request details.</CardDescription>
            </CardHeader>

            <CardContent className="space-y-4 text-sm">
              <MetadataRow
                label="File name"
                value={analysis.originalFileName}
              />
              <MetadataRow
                label="File size"
                value={formatFileSize(analysis.fileSizeBytes)}
              />
              <MetadataRow label="Content type" value={analysis.contentType} />
              <MetadataRow label="Model" value={analysis.modelName} />
              <MetadataRow
                label="Model version"
                value={analysis.modelVersion}
              />
              <MetadataRow
                label="Created"
                value={formatDateTime(analysis.createdAt)}
              />
              <MetadataRow
                label="Completed"
                value={formatDateTime(analysis.completedAt)}
              />
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Processing status</CardTitle>
              <CardDescription>Current AI workflow state.</CardDescription>
            </CardHeader>

            <CardContent className="space-y-4">
              <AnalysisStatusBadge status={analysis.status} />

              {POLLING_STATUSES.includes(analysis.status) && (
                <p className="text-sm text-muted-foreground">
                  This page refreshes automatically while the analysis is being
                  processed.
                </p>
              )}

              {analysis.status === "FAILED" && (
                <Button
                  type="button"
                  variant="outline"
                  className="w-full"
                  onClick={() => retryMutation.mutate(analysis.id)}
                  disabled={retryMutation.isPending}
                >
                  <RotateCcw
                    className={
                      retryMutation.isPending ? "size-4 animate-spin" : "size-4"
                    }
                  />
                  {retryMutation.isPending ? "Retrying..." : "Retry analysis"}
                </Button>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}

function AnalysisDetailsSkeleton() {
  return (
    <div className="space-y-6">
      <div className="space-y-3">
        <Skeleton className="h-10 w-72" />
        <Skeleton className="h-5 w-96" />
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
        <div className="space-y-6">
          <Skeleton className="h-96 w-full" />
          <Skeleton className="h-64 w-full" />
        </div>

        <Skeleton className="h-96 w-full" />
      </div>
    </div>
  );
}

type ImagePreviewProps = {
  title: string;
  imageUrl: string | null;
  fallback: string;
};

function ImagePreview({ title, imageUrl, fallback }: ImagePreviewProps) {
  return (
    <div className="space-y-3">
      <h3 className="text-sm font-medium">{title}</h3>

      {imageUrl ? (
        <div className="overflow-hidden rounded-lg border bg-muted">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src={imageUrl}
            alt={title}
            className="aspect-video w-full object-contain"
          />
        </div>
      ) : (
        <div className="flex aspect-video items-center justify-center rounded-lg border border-dashed bg-muted/40 p-6 text-center">
          <div>
            <FileImage className="mx-auto size-8 text-muted-foreground" />
            <p className="mt-2 text-sm text-muted-foreground">{fallback}</p>
          </div>
        </div>
      )}
    </div>
  );
}

type MetadataRowProps = {
  label: string;
  value: string;
};

function MetadataRow({ label, value }: MetadataRowProps) {
  return (
    <div className="flex items-start justify-between gap-4 border-b pb-3 last:border-b-0 last:pb-0">
      <span className="text-muted-foreground">{label}</span>
      <span className="text-right font-medium break-all">{value}</span>
    </div>
  );
}
