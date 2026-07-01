import {
  AlertTriangle,
  CheckCircle2,
  Clock3,
  FileImage,
  Users,
} from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

const summaryItems = [
  {
    title: "Patients",
    value: "—",
    description: "Total registered patients",
    icon: Users,
  },
  {
    title: "Analyses",
    value: "—",
    description: "Total submitted scans",
    icon: FileImage,
  },
  {
    title: "Completed",
    value: "—",
    description: "Finished AI analyses",
    icon: CheckCircle2,
  },
  {
    title: "Queued",
    value: "—",
    description: "Waiting for processing",
    icon: Clock3,
  },
  {
    title: "Failed",
    value: "—",
    description: "Analyses requiring attention",
    icon: AlertTriangle,
  },
];

export function SummaryCards() {
  return (
    <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
      {summaryItems.map((item) => {
        const Icon = item.icon;

        return (
          <Card key={item.title}>
            <CardHeader className="flex flex-row items-center justify-between gap-4 space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">
                {item.title}
              </CardTitle>

              <Icon className="size-4 text-muted-foreground" />
            </CardHeader>

            <CardContent>
              <div className="text-3xl font-bold">{item.value}</div>

              <p className="mt-1 text-xs text-muted-foreground">
                {item.description}
              </p>
            </CardContent>
          </Card>
        );
      })}
    </section>
  );
}
