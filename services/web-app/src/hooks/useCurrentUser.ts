"use client";

import { useQuery } from "@tanstack/react-query";

import { getCurrentUser } from "@/api/authApi";
import { queryKeys } from "@/lib/queryKeys";
import { useAuth } from "@/providers/AuthProvider";

export function useCurrentUser() {
  const { initialized, authenticated } = useAuth();

  return useQuery({
    queryKey: queryKeys.auth.me(),
    queryFn: getCurrentUser,
    enabled: initialized && authenticated,
    staleTime: 60_000,
  });
}
