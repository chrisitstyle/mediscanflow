"use client";

import Link from "next/link";
import { RefreshCw, UserPlus, Users } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { getUsers, updateUserStatus } from "@/api/userManagementApi";
import { AccessDenied } from "@/components/AccessDenied";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useCurrentUser } from "@/hooks/useCurrentUser";
import { ApiClientError } from "@/lib/apiClient";
import { canManageUsers } from "@/lib/permissions";
import { queryKeys } from "@/lib/queryKeys";
import { cn } from "@/lib/utils";
import type { User, UserStatus } from "@/types/userManagement";

type UpdateUserStatusVariables = {
  user: User;
  status: UserStatus;
};

export function UsersPage() {
  const currentUserQuery = useCurrentUser();
  const currentUser = currentUserQuery.data;

  const usersQuery = useQuery({
    queryKey: queryKeys.users.list(),
    queryFn: getUsers,
    enabled: Boolean(currentUser && canManageUsers(currentUser)),
  });

  if (currentUserQuery.isLoading) {
    return <UsersPageSkeleton />;
  }

  if (!currentUser || !canManageUsers(currentUser)) {
    return (
      <AccessDenied
        title="Admin access required"
        description="Only administrators can manage user accounts."
      />
    );
  }

  return (
    <UsersPageContent
      currentUserId={currentUser.id}
      users={usersQuery.data ?? []}
      isLoading={usersQuery.isLoading}
      isError={usersQuery.isError}
      error={usersQuery.error}
      onRetry={() => void usersQuery.refetch()}
    />
  );
}

type UsersPageContentProps = {
  currentUserId: string;
  users: User[];
  isLoading: boolean;
  isError: boolean;
  error: Error | null;
  onRetry: () => void;
};

function UsersPageContent({
  currentUserId,
  users,
  isLoading,
  isError,
  error,
  onRetry,
}: UsersPageContentProps) {
  const queryClient = useQueryClient();

  const activeAdminCount = users.filter(
    (user) => user.status === "Enabled" && user.roles.includes("ADMIN"),
  ).length;

  const mutation = useMutation({
    mutationFn: ({ user, status }: UpdateUserStatusVariables) =>
      updateUserStatus(user.id, { status }),

    onSuccess: async (updatedUser) => {
      queryClient.setQueryData<User[]>(
        queryKeys.users.list(),
        (previousUsers) =>
          previousUsers?.map((user) =>
            user.id === updatedUser.id ? updatedUser : user,
          ) ?? [updatedUser],
      );

      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: queryKeys.users.all,
        }),
        queryClient.invalidateQueries({
          queryKey: queryKeys.audit.recent(10),
        }),
        queryClient.invalidateQueries({
          queryKey: ["audit"],
        }),
      ]);

      toast.success("User status updated", {
        description: `${updatedUser.email} is now ${updatedUser.status}.`,
      });
    },

    onError: (error) => {
      toast.error("Could not update user status", {
        description: getStatusUpdateErrorMessage(error),
      });
    },
  });

  function handleStatusChange(user: User) {
    const nextStatus = getNextStatus(user.status);

    mutation.mutate({
      user,
      status: nextStatus,
    });
  }

  return (
    <main className="mx-auto flex w-full max-w-6xl flex-col gap-6 px-6 py-8">
      <section className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
        <div className="space-y-2">
          <Badge variant="outline" className="w-fit">
            Admin
          </Badge>

          <div className="space-y-1">
            <h1 className="text-3xl font-semibold tracking-tight">Users</h1>
            <p className="max-w-2xl text-sm text-muted-foreground">
              Review application users, roles and account status. Admins cannot
              disable their own account or the last active admin account.
            </p>
          </div>
        </div>

        <div className="flex flex-wrap gap-2">
          <Button
            type="button"
            variant="outline"
            onClick={onRetry}
            disabled={isLoading}
          >
            <RefreshCw
              className={cn("size-4", isLoading && "animate-spin")}
              aria-hidden="true"
            />
            Refresh
          </Button>

          <Button asChild variant="outline">
            <Link href="/admin/users/new">
              <UserPlus className="size-4" aria-hidden="true" />
              Create user
            </Link>
          </Button>
        </div>
      </section>

      {isError && (
        <Alert variant="destructive">
          <AlertTitle>Could not load users</AlertTitle>
          <AlertDescription>
            {getStatusUpdateErrorMessage(error)}
          </AlertDescription>
        </Alert>
      )}

      <Card>
        <CardHeader className="flex flex-row items-center justify-between gap-4">
          <CardTitle className="flex items-center gap-2">
            <Users className="size-5" aria-hidden="true" />
            User accounts
          </CardTitle>

          <Badge variant="secondary">
            {users.length} {users.length === 1 ? "user" : "users"}
          </Badge>
        </CardHeader>

        <CardContent>
          {isLoading ? (
            <UsersTableSkeleton />
          ) : (
            <UsersTable
              users={users}
              currentUserId={currentUserId}
              activeAdminCount={activeAdminCount}
              pendingUserId={mutation.variables?.user.id}
              isUpdating={mutation.isPending}
              onStatusChange={handleStatusChange}
            />
          )}
        </CardContent>
      </Card>
    </main>
  );
}

type UsersTableProps = {
  users: User[];
  currentUserId: string;
  activeAdminCount: number;
  pendingUserId: string | undefined;
  isUpdating: boolean;
  onStatusChange: (user: User) => void;
};

function UsersTable({
  users,
  currentUserId,
  activeAdminCount,
  pendingUserId,
  isUpdating,
  onStatusChange,
}: UsersTableProps) {
  if (users.length === 0) {
    return (
      <div className="rounded-lg border border-dashed p-8 text-center">
        <p className="font-medium">No users found</p>
        <p className="mt-1 text-sm text-muted-foreground">
          Created users will appear here.
        </p>
      </div>
    );
  }

  return (
    <div className="overflow-hidden rounded-lg border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>User</TableHead>
            <TableHead>Email</TableHead>
            <TableHead>Roles</TableHead>
            <TableHead>Status</TableHead>
            <TableHead className="text-right">Actions</TableHead>
          </TableRow>
        </TableHeader>

        <TableBody>
          {users.map((user) => {
            const isCurrentUser = user.id === currentUserId;
            const isPending = isUpdating && pendingUserId === user.id;
            const wouldDisableLastAdmin =
              user.status === "Enabled" &&
              user.roles.includes("ADMIN") &&
              activeAdminCount <= 1;

            const disableReason = getDisableReason({
              user,
              isCurrentUser,
              wouldDisableLastAdmin,
            });

            return (
              <TableRow key={user.id}>
                <TableCell>
                  <div className="flex flex-col">
                    <span className="font-medium">
                      {getUserDisplayName(user)}
                    </span>

                    {isCurrentUser && (
                      <span className="text-xs text-muted-foreground">
                        Current user
                      </span>
                    )}
                  </div>
                </TableCell>

                <TableCell className="text-muted-foreground">
                  {user.email}
                </TableCell>

                <TableCell>
                  <div className="flex flex-wrap gap-1">
                    {user.roles.map((role) => (
                      <Badge key={role} variant="outline">
                        {role}
                      </Badge>
                    ))}
                  </div>
                </TableCell>

                <TableCell>
                  <UserStatusBadge status={user.status} />
                </TableCell>

                <TableCell className="text-right">
                  <div className="flex justify-end">
                    <Button
                      type="button"
                      size="sm"
                      variant={
                        user.status === "Enabled" ? "destructive" : "outline"
                      }
                      disabled={isPending || Boolean(disableReason)}
                      title={disableReason}
                      onClick={() => onStatusChange(user)}
                    >
                      {getActionLabel(user.status, isPending)}
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
}

type UserStatusBadgeProps = {
  status: UserStatus;
};

function UserStatusBadge({ status }: UserStatusBadgeProps) {
  if (status === "Enabled") {
    return <Badge variant="secondary">Enabled</Badge>;
  }

  return <Badge variant="outline">Disabled</Badge>;
}

type DisableReasonInput = {
  user: User;
  isCurrentUser: boolean;
  wouldDisableLastAdmin: boolean;
};

function getDisableReason({
  user,
  isCurrentUser,
  wouldDisableLastAdmin,
}: DisableReasonInput) {
  if (user.status === "Disabled") {
    return undefined;
  }

  if (isCurrentUser) {
    return "You cannot disable your own account.";
  }

  if (wouldDisableLastAdmin) {
    return "You cannot disable the last active admin account.";
  }

  return undefined;
}

function getNextStatus(status: UserStatus): UserStatus {
  return status === "Enabled" ? "Disabled" : "Enabled";
}

function getActionLabel(status: UserStatus, isPending: boolean) {
  if (isPending) {
    return "Updating...";
  }

  return status === "Enabled" ? "Disable" : "Enable";
}

function getUserDisplayName(user: User) {
  const fullName = [user.firstName, user.lastName]
    .filter(Boolean)
    .join(" ")
    .trim();

  return fullName || user.email;
}

function getStatusUpdateErrorMessage(error: unknown) {
  if (error instanceof ApiClientError) {
    if (error.status === 400) {
      return error.message || "This status change is not allowed.";
    }

    if (error.status === 403) {
      return "You do not have permission to manage users.";
    }

    if (error.status === 404) {
      return "User was not found.";
    }

    if (error.status === 409) {
      return (
        error.message || "This status change conflicts with business rules."
      );
    }

    return error.message;
  }

  return "Unexpected error. Please try again.";
}

function UsersPageSkeleton() {
  return (
    <main className="mx-auto flex w-full max-w-6xl flex-col gap-6 px-6 py-8">
      <div className="space-y-3">
        <Skeleton className="h-5 w-20" />
        <Skeleton className="h-9 w-48" />
        <Skeleton className="h-4 w-full max-w-xl" />
      </div>

      <UsersTableSkeleton />
    </main>
  );
}

function UsersTableSkeleton() {
  return (
    <div className="space-y-3">
      {Array.from({ length: 5 }).map((_, index) => (
        <Skeleton key={index} className="h-14 w-full" />
      ))}
    </div>
  );
}
