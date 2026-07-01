import { Activity, Circle } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

const components = [
  "Backend API",
  "Database",
  "RabbitMQ",
  "MinIO",
  "AI Worker",
];

export function SystemStatusCard() {
  return (
    <Card>
      <CardHeader>
        <div className="flex items-start justify-between gap-4">
          <div>
            <CardTitle className="flex items-center gap-2">
              <Activity className="size-5" />
              System status
            </CardTitle>

            <CardDescription>
              Health overview of MediScanFlow services.
            </CardDescription>
          </div>

          <Badge variant="outline">Pending</Badge>
        </div>
      </CardHeader>

      <CardContent>
        <div className="space-y-3">
          {components.map((component) => (
            <div
              key={component}
              className="flex items-center justify-between rounded-lg border px-3 py-2"
            >
              <div className="flex items-center gap-2">
                <Circle className="size-3 fill-muted text-muted-foreground" />
                <span className="text-sm font-medium">{component}</span>
              </div>

              <span className="text-xs text-muted-foreground">—</span>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
