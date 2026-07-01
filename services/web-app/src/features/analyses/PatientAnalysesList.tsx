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
          <div className="overflow-hidden rounded-lg border">
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
