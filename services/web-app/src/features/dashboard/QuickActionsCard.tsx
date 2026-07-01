import Link from "next/link";
import { Plus, Users } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

export function QuickActionsCard() {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Quick actions</CardTitle>
        <CardDescription>
          Start common workflows from the dashboard.
        </CardDescription>
      </CardHeader>

      <CardContent className="flex flex-col gap-3">
        <Button asChild>
          <Link href="/patients/new">
            <Plus className="size-4" />
            Create patient
          </Link>
        </Button>

        <Button asChild variant="outline">
          <Link href="/patients">
            <Users className="size-4" />
            Open patient registry
          </Link>
        </Button>
      </CardContent>
    </Card>
  );
}
