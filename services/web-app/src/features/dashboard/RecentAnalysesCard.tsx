import Link from "next/link";
import { FileImage } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

export function RecentAnalysesCard() {
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

          <Badge variant="outline">Coming soon</Badge>
        </div>
      </CardHeader>

      <CardContent>
        <div className="flex min-h-80 flex-col items-center justify-center rounded-lg border border-dashed p-10 text-center">
          <div className="flex size-12 items-center justify-center rounded-full bg-muted">
            <FileImage className="size-6 text-muted-foreground" />
          </div>

          <h2 className="mt-4 text-lg font-semibold">
            Recent analyses will appear here
          </h2>

          <p className="mt-2 max-w-md text-sm text-muted-foreground">
            In the next step, this section will show the latest analysis jobs,
            their status, model metadata and links to result details.
          </p>

          <Button asChild className="mt-6" variant="outline">
            <Link href="/patients">Open patients</Link>
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
