import type { ReactNode } from "react";

import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

import { STATUS_TONE_STYLES, type StatusTone } from "./statusStyles";

type StatusBadgeProps = {
  children: ReactNode;
  tone?: StatusTone;
  className?: string;
};

export function StatusBadge({
  children,
  tone = "neutral",
  className,
}: StatusBadgeProps) {
  return (
    <Badge
      variant="outline"
      className={cn(
        "rounded-full px-2 font-semibold uppercase tracking-wide",
        STATUS_TONE_STYLES[tone],
        className,
      )}
    >
      {children}
    </Badge>
  );
}
