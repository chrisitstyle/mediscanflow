"use client";

import Link from "next/link";
import { Plus, UserPlus, Users } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { useCurrentUser } from "@/hooks/useCurrentUser";
import { canManageUsers, canWriteMedicalData } from "@/lib/permissions";

export function QuickActionsCard() {
  const currentUserQuery = useCurrentUser();
  const currentUser = currentUserQuery.data;

  const canWrite = canWriteMedicalData(currentUser);
  const canManage = canManageUsers(currentUser);

  return (
    <Card>
      <CardHeader>
        <CardTitle>Quick actions</CardTitle>
        <CardDescription>
          {canWrite
            ? "Start common workflows from the dashboard."
            : "Open available read-only workflows from the dashboard."}
        </CardDescription>
      </CardHeader>

      <CardContent className="flex flex-col gap-3">
        {canWrite && (
          <Button asChild variant="outline" className="w-full gap-2">
            <Link href="/patients/new">
              <Plus className="size-4" />
              Create patient
            </Link>
          </Button>
        )}

        {canManage && (
          <Button asChild variant="outline" className="w-full gap-2">
            <Link href="/admin/users/new">
              <UserPlus className="size-4" />
              Create user
            </Link>
          </Button>
        )}

        <Button asChild variant={canWrite || canManage ? "outline" : "default"}>
          <Link href="/patients">
            <Users className="size-4" />
            Open patient registry
          </Link>
        </Button>
      </CardContent>
    </Card>
  );
}
