"use client";

import Link from "next/link";
import { FileImage } from "lucide-react";

import { AnalysisStatusBadge } from "@/components/status/AnalysisStatusBadge";
import { EmptyState } from "@/components/EmptyState";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import type { Analysis } from "@/types/analysis";

type PatientAnalysesListProps = {
  analyses: Analysis[];
};

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

export function PatientAnalysesList({ analyses }: PatientAnalysesListProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Analyses</CardTitle>
        <CardDescription>
          Medical scan analyses submitted for this patient.
        </CardDescription>
      </CardHeader>

      <CardContent>
        {analyses.length === 0 ? (
          <EmptyState
            icon={<FileImage className="size-6" />}
            title="No analyses yet"
            description="Upload the first scan for this patient to start asynchronous AI analysis."
          />
        ) : (
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
                        ID: {analysis.id}
                      </p>
                    </div>

                    <AnalysisStatusBadge status={analysis.status} />
                  </div>

                  <div className="mt-4 grid gap-3 text-sm">
                    <MobileMetaRow
                      label="Model"
                      value={`${analysis.modelName} / ${analysis.modelVersion}`}
                    />
                    <MobileMetaRow
                      label="Size"
                      value={formatFileSize(analysis.fileSizeBytes)}
                    />
                    <MobileMetaRow
                      label="Created"
                      value={formatDateTime(analysis.createdAt)}
                    />
                  </div>

                  <Button
                    asChild
                    variant="outline"
                    size="sm"
                    className="mt-4 w-full"
                  >
                    <Link href={`/analyses/${analysis.id}`}>View details</Link>
                  </Button>
                </div>
              ))}
            </div>

            <div className="hidden overflow-hidden rounded-lg border md:block">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>File</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Model</TableHead>
                    <TableHead>Size</TableHead>
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
                          ID: {analysis.id}
                        </div>
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
                        {formatFileSize(analysis.fileSizeBytes)}
                      </TableCell>

                      <TableCell>
                        {formatDateTime(analysis.createdAt)}
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
      </CardContent>
    </Card>
  );
}

type MobileMetaRowProps = {
  label: string;
  value: string;
};

function MobileMetaRow({ label, value }: MobileMetaRowProps) {
  return (
    <div className="flex items-center justify-between gap-4">
      <span className="text-muted-foreground">{label}</span>
      <span className="text-right font-medium">{value}</span>
    </div>
  );
}
