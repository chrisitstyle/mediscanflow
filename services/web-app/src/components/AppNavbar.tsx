"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { LogOut, ScanLine } from "lucide-react";

import { SystemStatusPopover } from "@/features/system/SystemStatusPopover";
import { useCurrentUser } from "@/hooks/useCurrentUser";
import { canViewSystemStatus } from "@/lib/permissions";
import { cn } from "@/lib/utils";
import { useAuth } from "@/providers/AuthProvider";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";

const navItems = [
  { href: "/", label: "Dashboard" },
  { href: "/patients", label: "Patients" },
  { href: "/analyses", label: "Analyses" },
  { href: "/activity", label: "Activity" },
];

const ROLE_LABELS = {
  ADMIN: "Admin",
  DOCTOR: "Doctor",
  STAFF: "Staff",
} as const;

export function AppNavbar() {
  const pathname = usePathname();
  const { authenticated, logout } = useAuth();
  const currentUserQuery = useCurrentUser();

  const currentUser = currentUserQuery.data;
  const primaryRole = currentUser?.roles[0];

  if (pathname.startsWith("/login")) {
    return null;
  }

  return (
    <header className="sticky top-0 z-40 w-full border-b border-border/60 bg-background/80 backdrop-blur-xl supports-backdrop-filter:bg-background/60">
      <div className="mx-auto flex h-16 w-full max-w-6xl items-center justify-between gap-6 px-6">
        <div className="flex min-w-0 items-center gap-8">
          <Link
            href="/"
            className="group flex shrink-0 items-center gap-2.5 transition-opacity hover:opacity-90"
          >
            <span className="flex size-9 items-center justify-center rounded-xl bg-primary text-primary-foreground shadow-sm shadow-primary/25 ring-1 ring-inset ring-primary/20 transition-transform group-hover:scale-105">
              <ScanLine className="size-5" aria-hidden="true" />
            </span>

            <span className="text-base font-semibold tracking-tight">
              MediScan
              <span className="text-primary">Flow</span>
            </span>
          </Link>

          <nav
            className="hidden items-center gap-1 rounded-full border border-border/60 bg-muted/40 p-1 md:flex"
            aria-label="Main"
          >
            {navItems.map((item) => {
              const isActive =
                item.href === "/"
                  ? pathname === "/"
                  : pathname.startsWith(item.href);

              return (
                <Link
                  key={item.href}
                  href={item.href}
                  aria-current={isActive ? "page" : undefined}
                  className={cn(
                    "rounded-full px-4 py-1.5 text-sm font-medium text-muted-foreground transition-all hover:text-foreground",
                    isActive &&
                      "bg-background text-foreground shadow-sm ring-1 ring-border/60",
                  )}
                >
                  {item.label}
                </Link>
              );
            })}
          </nav>
        </div>

        <div className="flex shrink-0 items-center gap-3">
          {canViewSystemStatus(currentUser) && <SystemStatusPopover />}

          {authenticated && currentUser && (
            <div className="hidden items-center gap-2 rounded-full border border-border/60 bg-background/80 px-3 py-1.5 shadow-sm sm:flex">
              <div className="max-w-44 leading-tight">
                <div className="truncate text-sm font-medium">
                  {currentUser.firstName} {currentUser.lastName}
                </div>

                <div className="truncate text-xs text-muted-foreground">
                  {currentUser.email}
                </div>
              </div>

              {primaryRole && (
                <Badge
                  variant="secondary"
                  className="rounded-full px-2 text-[10px] font-semibold uppercase tracking-wide"
                >
                  {ROLE_LABELS[primaryRole] ?? primaryRole}
                </Badge>
              )}
            </div>
          )}

          {authenticated && currentUser && (
            <div className="flex items-center gap-2 sm:hidden">
              {primaryRole && (
                <Badge
                  variant="secondary"
                  className="rounded-full px-2 text-[10px] font-semibold uppercase tracking-wide"
                >
                  {ROLE_LABELS[primaryRole] ?? primaryRole}
                </Badge>
              )}
            </div>
          )}

          {authenticated && (
            <Button
              type="button"
              variant="ghost"
              size="icon"
              onClick={() => void logout()}
              aria-label="Logout"
              title="Logout"
              className="rounded-full"
            >
              <LogOut className="size-4" />
            </Button>
          )}
        </div>
      </div>
    </header>
  );
}
