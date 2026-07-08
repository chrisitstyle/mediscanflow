"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { LogOut, Menu, ScanLine, X } from "lucide-react";

import { SystemStatusPopover } from "@/features/system/SystemStatusPopover";
import { useCurrentUser } from "@/hooks/useCurrentUser";
import { canManageUsers, canViewSystemStatus } from "@/lib/permissions";
import { cn } from "@/lib/utils";
import { useAuth } from "@/providers/AuthProvider";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";

const baseNavItems = [
  { href: "/", label: "Dashboard" },
  { href: "/patients", label: "Patients" },
  { href: "/analyses", label: "Analyses" },
  { href: "/activity", label: "Activity" },
];

const adminNavItems = [{ href: "/admin/users", label: "Users" }];

const ROLE_LABELS = {
  ADMIN: "Admin",
  DOCTOR: "Doctor",
  STAFF: "Staff",
} as const;

export function AppNavbar() {
  const pathname = usePathname();
  const { authenticated, logout } = useAuth();
  const currentUserQuery = useCurrentUser();

  const [mobileOpen, setMobileOpen] = useState(false);

  const currentUser = currentUserQuery.data;
  const primaryRole = currentUser?.roles[0];

  const navItems = [
    ...baseNavItems,
    ...(currentUser && canManageUsers(currentUser) ? adminNavItems : []),
  ];

  const isActive = (href: string) =>
    href === "/" ? pathname === "/" : pathname.startsWith(href);

  const closeMobileMenu = () => setMobileOpen(false);

  // lock body scroll and close on Escape while the mobile menu is open.
  useEffect(() => {
    if (!mobileOpen) return;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") closeMobileMenu();
    };

    document.addEventListener("keydown", onKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [mobileOpen]);

  if (pathname.startsWith("/login")) {
    return null;
  }

  return (
    <header className="sticky top-0 z-40 w-full border-b border-border/60 bg-background/80 backdrop-blur-xl supports-backdrop-filter:bg-background/60">
      <div className="mx-auto flex h-16 w-full max-w-6xl items-center justify-between gap-3 px-4 sm:gap-6 sm:px-6">
        <div className="flex min-w-0 items-center gap-4 lg:gap-8">
          <Link
            href="/"
            onClick={closeMobileMenu}
            className="group flex min-w-0 items-center gap-2.5 transition-opacity hover:opacity-90"
          >
            <span className="flex size-9 shrink-0 items-center justify-center rounded-xl bg-primary text-primary-foreground shadow-sm shadow-primary/25 ring-1 ring-inset ring-primary/20 transition-transform group-hover:scale-105">
              <ScanLine className="size-5" aria-hidden="true" />
            </span>

            <span className="truncate text-base font-semibold tracking-tight">
              MediScan
              <span className="text-primary">Flow</span>
            </span>
          </Link>

          <nav
            className="hidden items-center gap-1 rounded-full border border-border/60 bg-muted/40 p-1 md:flex"
            aria-label="Main"
          >
            {navItems.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                aria-current={isActive(item.href) ? "page" : undefined}
                className={cn(
                  "rounded-full px-4 py-1.5 text-sm font-medium text-muted-foreground transition-all hover:text-foreground",
                  isActive(item.href) &&
                    "bg-background text-foreground shadow-sm ring-1 ring-border/60",
                )}
              >
                {item.label}
              </Link>
            ))}
          </nav>
        </div>

        <div className="flex shrink-0 items-center gap-2 sm:gap-3">
          {canViewSystemStatus(currentUser) && <SystemStatusPopover />}

          {/* full user card — only on wide enough screens */}
          {authenticated && currentUser && (
            <div className="hidden items-center gap-2 rounded-full border border-border/60 bg-background/80 px-3 py-1.5 shadow-sm lg:flex">
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

          {/* compact role badge — tablet / small desktop */}
          {authenticated && currentUser && primaryRole && (
            <Badge
              variant="secondary"
              className="hidden rounded-full px-2 text-[10px] font-semibold uppercase tracking-wide sm:inline-flex lg:hidden"
            >
              {ROLE_LABELS[primaryRole] ?? primaryRole}
            </Badge>
          )}

          {/* logout — hidden on mobile, available inside the mobile drawer instead */}
          {authenticated && (
            <Button
              type="button"
              variant="ghost"
              size="icon"
              onClick={() => void logout()}
              aria-label="Logout"
              title="Logout"
              className="hidden rounded-full md:inline-flex"
            >
              <LogOut className="size-4" />
            </Button>
          )}

          {/* hamburger - mobile only, toggles the drawer */}
          <Button
            type="button"
            variant="ghost"
            size="icon"
            aria-label={mobileOpen ? "Close menu" : "Open menu"}
            aria-expanded={mobileOpen}
            aria-controls="mobile-nav"
            onClick={() => setMobileOpen((open) => !open)}
            className="rounded-full md:hidden"
          >
            {mobileOpen ? (
              <X className="size-5" />
            ) : (
              <Menu className="size-5" />
            )}
          </Button>
        </div>
      </div>

      {/* mobile navigation drawer */}
      {mobileOpen && (
        <div className="md:hidden">
          {/* Backdrop */}
          <button
            type="button"
            aria-label="Close menu"
            onClick={closeMobileMenu}
            className="fixed inset-0 top-16 z-40 bg-foreground/20 backdrop-blur-sm"
          />

          {/* panel */}
          <nav
            id="mobile-nav"
            aria-label="Mobile"
            className="fixed inset-x-0 top-16 z-50 max-h-[calc(100dvh-4rem)] overflow-y-auto border-b border-border/60 bg-background p-4 shadow-lg"
          >
            {authenticated && currentUser && (
              <div className="mb-4 flex items-center justify-between gap-3 rounded-xl border border-border/60 bg-muted/40 px-3 py-2.5">
                <div className="min-w-0 leading-tight">
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
                    className="shrink-0 rounded-full px-2 text-[10px] font-semibold uppercase tracking-wide"
                  >
                    {ROLE_LABELS[primaryRole] ?? primaryRole}
                  </Badge>
                )}
              </div>
            )}

            <ul className="flex flex-col gap-1">
              {navItems.map((item) => (
                <li key={item.href}>
                  <Link
                    href={item.href}
                    aria-current={isActive(item.href) ? "page" : undefined}
                    onClick={closeMobileMenu}
                    className={cn(
                      "block rounded-lg px-3 py-2.5 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground",
                      isActive(item.href) &&
                        "bg-muted text-foreground ring-1 ring-border/60",
                    )}
                  >
                    {item.label}
                  </Link>
                </li>
              ))}
            </ul>

            {authenticated && (
              <Button
                type="button"
                variant="outline"
                onClick={() => {
                  closeMobileMenu();
                  void logout();
                }}
                className="mt-4 w-full justify-start gap-2"
              >
                <LogOut className="size-4" />
                Logout
              </Button>
            )}
          </nav>
        </div>
      )}
    </header>
  );
}
