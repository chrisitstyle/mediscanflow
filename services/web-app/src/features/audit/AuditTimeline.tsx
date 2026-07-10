"use client";

import Link from "next/link";
import { Activity, FileText } from "lucide-react";
import { useQuery } from "@tanstack/react-query";

import {
  getAnalysisAuditEvents,
  getPatientAuditEvents,
  getRecentAuditEvents,
} from "@/api/auditApi";
import { EmptyState } from "@/components/EmptyState";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { AuditEventIcon } from "@/features/audit/AuditEventIcon";
import { AUDIT_EVENT_LABELS } from "@/features/audit/auditEventPresentation";
import { ApiClientError } from "@/lib/apiClient";
import { formatDateTime } from "@/lib/formatters";
import { queryKeys } from "@/lib/queryKeys";
import type { AuditEvent } from "@/types/audit";

type AuditTimelineBaseProps = {
  limit?: number;
  title?: string;
  description?: string;
  emptyTitle?: string;
  emptyDescription?: string;
  refetchInterval?: number | false;
  showLinks?: boolean;
};

type RecentAuditTimelineProps = AuditTimelineBaseProps & {
  scope: "recent";
};

type PatientAuditTimelineProps = AuditTimelineBaseProps & {
  scope: "patient";
  patientId: string;
};

type AnalysisAuditTimelineProps = AuditTimelineBaseProps & {
  scope: "analysis";
  analysisId: string;
};

type AuditTimelineProps =
  | RecentAuditTimelineProps
  | PatientAuditTimelineProps
  | AnalysisAuditTimelineProps;

export function AuditTimeline(props: AuditTimelineProps) {
  const limit = props.limit ?? 10;
  const showLinks = props.showLinks ?? true;

  const { data, isLoading, isError, error } = useQuery({
    queryKey: getAuditQueryKey(props, limit),
    queryFn: () => getAuditEvents(props, limit),
    refetchInterval: props.refetchInterval ?? 15_000,
  });

  const events = data ?? [];
  const hasEvents = events.length > 0;

  const title = props.title ?? getDefaultTitle(props.scope);
  const description = props.description ?? getDefaultDescription(props.scope);
  const emptyTitle = props.emptyTitle ?? "No activity yet";
  const emptyDescription =
    props.emptyDescription ??
    "Audit events will appear here after users perform clinical actions.";

  const errorMessage =
    error instanceof ApiClientError
      ? error.message
      : "Could not load audit events";

  return (
    <Card>
      <CardHeader>
        <div className="flex items-start justify-between gap-4">
          <div>
            <CardTitle>{title}</CardTitle>
            <CardDescription>{description}</CardDescription>
          </div>

          <Badge variant="outline">Latest {limit}</Badge>
        </div>
      </CardHeader>

      <CardContent>
        {isLoading && <AuditTimelineSkeleton />}

        {isError && (
          <Alert variant="destructive">
            <AlertTitle>Could not load activity</AlertTitle>
            <AlertDescription>{errorMessage}</AlertDescription>
          </Alert>
        )}

        {!isLoading && !isError && !hasEvents && (
          <EmptyState
            icon={<Activity className="size-6" />}
            title={emptyTitle}
            description={emptyDescription}
            className="min-h-64"
          />
        )}

        {!isLoading && !isError && hasEvents && (
          <div className="space-y-4">
            {events.map((event, index) => (
              <AuditTimelineItem
                key={event.id}
                event={event}
                isLast={index === events.length - 1}
                showLinks={showLinks}
              />
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

type AuditTimelineItemProps = {
  event: AuditEvent;
  isLast: boolean;
  showLinks: boolean;
};

function AuditTimelineItem({
  event,
  isLast,
  showLinks,
}: AuditTimelineItemProps) {
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
            <div className="flex flex-wrap items-center gap-2">
              <h3 className="font-medium">{AUDIT_EVENT_LABELS[event.type]}</h3>
            </div>

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

          {showLinks && <AuditEventLinks event={event} />}
        </div>
      </div>
    </div>
  );
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

function getAuditQueryKey(props: AuditTimelineProps, limit: number) {
  switch (props.scope) {
    case "patient":
      return queryKeys.audit.patient(props.patientId, limit);
    case "analysis":
      return queryKeys.audit.analysis(props.analysisId, limit);
    case "recent":
      return queryKeys.audit.recent(limit);
  }
}

function getAuditEvents(
  props: AuditTimelineProps,
  limit: number,
): Promise<AuditEvent[]> {
  switch (props.scope) {
    case "patient":
      return getPatientAuditEvents(props.patientId, { limit });
    case "analysis":
      return getAnalysisAuditEvents(props.analysisId, { limit });
    case "recent":
      return getRecentAuditEvents({ limit });
  }
}

function getDefaultTitle(scope: AuditTimelineProps["scope"]) {
  switch (scope) {
    case "patient":
      return "Patient activity";
    case "analysis":
      return "Analysis activity";
    case "recent":
      return "Recent activity";
  }
}

function getDefaultDescription(scope: AuditTimelineProps["scope"]) {
  switch (scope) {
    case "patient":
      return "Audit trail for this patient record.";
    case "analysis":
      return "Audit trail for this scan analysis.";
    case "recent":
      return "Latest user actions across the platform.";
  }
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

function AuditTimelineSkeleton() {
  return (
    <div className="space-y-4">
      <Skeleton className="h-16 w-full" />
      <Skeleton className="h-16 w-full" />
      <Skeleton className="h-16 w-full" />
    </div>
  );
}
