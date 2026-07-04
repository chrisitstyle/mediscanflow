"use client";

import Link from "next/link";
import {
  Activity,
  Archive,
  Download,
  FileText,
  RefreshCcw,
  RotateCcw,
  UploadCloud,
  UserCog,
  UserPlus,
  UserRoundPen,
} from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { useRouter, useSearchParams } from "next/navigation";

import { getAuditEventsPage } from "@/api/auditApi";
import { EmptyState } from "@/components/EmptyState";
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
import { ApiClientError } from "@/lib/apiClient";
import { queryKeys } from "@/lib/queryKeys";
import { formatDateTime } from "@/lib/formatters";
import type { AuditEvent, AuditEventType } from "@/types/audit";

const PAGE_SIZE = 50;

const AUDIT_EVENT_LABELS: Record<AuditEventType, string> = {
  PATIENT_CREATED: "Patient created",
  PATIENT_PROFILE_UPDATED: "Patient updated",
  PATIENT_ARCHIVED: "Patient archived",
  PATIENT_RESTORED: "Patient restored",
  ANALYSIS_UPLOADED: "Analysis uploaded",
  ANALYSIS_RETRIED: "Analysis retried",
  REPORT_DOWNLOADED: "Report downloaded",
  USER_CREATED: "User created",
};

function resolvePage(value: string | null) {
  if (!value) {
    return 0;
  }

  const page = Number(value);

  if (!Number.isInteger(page) || page < 0) {
    return 0;
  }

  return page;
}

export function ActivityPage() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const page = resolvePage(searchParams.get("page"));

  const { data, isLoading, isError, error, isFetching } = useQuery({
    queryKey: queryKeys.audit.page(page, PAGE_SIZE),
    queryFn: () =>
      getAuditEventsPage({
        page,
        size: PAGE_SIZE,
      }),
    placeholderData: (previousData) => previousData,
    refetchInterval: 15_000,
  });

  const events = data?.content ?? [];
  const hasEvents = events.length > 0;

  const errorMessage =
    error instanceof ApiClientError
      ? error.message
      : "Could not load activity log";

  function goToPage(nextPage: number) {
    const params = new URLSearchParams(searchParams.toString());

    if (nextPage <= 0) {
      params.delete("page");
    } else {
      params.set("page", String(nextPage));
    }

    const query = params.toString();

    router.push(`/activity${query ? `?${query}` : ""}`);
  }

  return (
    <main className="mx-auto flex w-full max-w-6xl flex-col gap-6 px-6 py-8">
      <div>
        <Badge variant="secondary">Activity</Badge>

        <h1 className="mt-4 text-3xl font-bold tracking-tight">Activity log</h1>

        <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
          Review audit events and user actions across MediScanFlow.
        </p>
      </div>

      <Card>
        <CardHeader>
          <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <CardTitle>Audit trail</CardTitle>
              <CardDescription>
                Latest recorded user actions across patients, analyses and
                reports.
              </CardDescription>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <Badge variant="outline">{PAGE_SIZE} events per page</Badge>

              {data && (
                <Badge variant="outline">
                  Page {data.totalPages === 0 ? 0 : data.page + 1} of{" "}
                  {data.totalPages}
                </Badge>
              )}

              {isFetching && !isLoading && (
                <Badge variant="secondary">Refreshing</Badge>
              )}
            </div>
          </div>
        </CardHeader>

        <CardContent>
          {isLoading && <ActivitySkeleton />}

          {isError && (
            <Alert variant="destructive">
              <AlertTitle>Could not load activity log</AlertTitle>
              <AlertDescription>{errorMessage}</AlertDescription>
            </Alert>
          )}

          {!isLoading && !isError && !hasEvents && (
            <EmptyState
              icon={<Activity className="size-6" />}
              title="No activity yet"
              description="Audit events will appear here after users create patients, update profiles, upload scans, retry analyses or download reports."
              className="min-h-80"
            />
          )}

          {!isLoading && !isError && hasEvents && (
            <>
              <div className="space-y-4">
                {events.map((event, index) => (
                  <ActivityTimelineItem
                    key={event.id}
                    event={event}
                    isLast={index === events.length - 1}
                  />
                ))}
              </div>

              {data && (
                <ActivityPagination
                  page={data.page}
                  totalPages={data.totalPages}
                  first={data.first}
                  last={data.last}
                  totalElements={data.totalElements}
                  onPrevious={() => goToPage(data.page - 1)}
                  onNext={() => goToPage(data.page + 1)}
                />
              )}
            </>
          )}
        </CardContent>
      </Card>
    </main>
  );
}

type ActivityTimelineItemProps = {
  event: AuditEvent;
  isLast: boolean;
};

function ActivityTimelineItem({ event, isLast }: ActivityTimelineItemProps) {
  return (
    <div className="relative flex gap-4">
      <div className="flex flex-col items-center">
        <div className="flex size-9 items-center justify-center rounded-full border bg-background">
          <AuditEventIcon type={event.type} />
        </div>

        {!isLast && <div className="mt-2 h-full w-px bg-border" />}
      </div>

      <div className="min-w-0 flex-1 pb-4">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
          <div className="min-w-0">
            <h3 className="font-medium">{AUDIT_EVENT_LABELS[event.type]}</h3>

            <p className="mt-1 text-sm text-muted-foreground">
              {event.message}
            </p>
          </div>

          <time className="shrink-0 text-xs text-muted-foreground">
            {formatDateTime(event.createdAt)}
          </time>
        </div>

        <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-2 text-xs text-muted-foreground">
          <span>{formatActor(event)}</span>

          <AuditEventLinks event={event} />
        </div>
      </div>
    </div>
  );
}

type AuditEventIconProps = {
  type: AuditEventType;
};

function AuditEventIcon({ type }: AuditEventIconProps) {
  const className = "size-4 text-muted-foreground";

  switch (type) {
    case "PATIENT_CREATED":
      return <UserPlus className={className} />;
    case "PATIENT_PROFILE_UPDATED":
      return <UserRoundPen className={className} />;
    case "PATIENT_ARCHIVED":
      return <Archive className={className} />;
    case "PATIENT_RESTORED":
      return <RotateCcw className={className} />;
    case "ANALYSIS_UPLOADED":
      return <UploadCloud className={className} />;
    case "ANALYSIS_RETRIED":
      return <RefreshCcw className={className} />;
    case "REPORT_DOWNLOADED":
      return <Download className={className} />;
    case "USER_CREATED":
      return <UserCog className={className} />;
  }
}

type AuditEventLinksProps = {
  event: AuditEvent;
};

function AuditEventLinks({ event }: AuditEventLinksProps) {
  return (
    <>
      {event.patientId && (
        <Link
          href={`/patients/${event.patientId}`}
          className="font-medium text-foreground hover:underline"
        >
          Patient
        </Link>
      )}

      {event.analysisId && (
        <Link
          href={`/analyses/${event.analysisId}`}
          className="inline-flex items-center gap-1 font-medium text-foreground hover:underline"
        >
          <FileText className="size-3" />
          Analysis
        </Link>
      )}
    </>
  );
}

type ActivityPaginationProps = {
  page: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  totalElements: number;
  onPrevious: () => void;
  onNext: () => void;
};

function ActivityPagination({
  page,
  totalPages,
  first,
  last,
  totalElements,
  onPrevious,
  onNext,
}: ActivityPaginationProps) {
  const currentPage = totalPages === 0 ? 0 : page + 1;

  return (
    <div className="mt-6 flex flex-col gap-4 border-t pt-6 sm:flex-row sm:items-center sm:justify-between">
      <p className="text-sm text-muted-foreground">
        Page <span className="font-medium text-foreground">{currentPage}</span>{" "}
        of <span className="font-medium text-foreground">{totalPages}</span> ·{" "}
        <span className="font-medium text-foreground">{totalElements}</span>{" "}
        total events
      </p>

      <div className="flex items-center gap-2">
        <Button
          type="button"
          variant="outline"
          onClick={onPrevious}
          disabled={first}
        >
          Previous
        </Button>

        <Button
          type="button"
          variant="outline"
          onClick={onNext}
          disabled={last}
        >
          Next
        </Button>
      </div>
    </div>
  );
}

function formatActor(event: AuditEvent) {
  const actor = event.actorEmail ?? "Unknown user";

  if (!event.actorRole) {
    return actor;
  }

  return `${actor} · ${formatRole(event.actorRole)}`;
}

function formatRole(role: string) {
  return role.toLowerCase().replaceAll("_", " ");
}

function ActivitySkeleton() {
  return (
    <div className="space-y-4">
      <Skeleton className="h-16 w-full" />
      <Skeleton className="h-16 w-full" />
      <Skeleton className="h-16 w-full" />
      <Skeleton className="h-16 w-full" />
      <Skeleton className="h-16 w-full" />
    </div>
  );
}
