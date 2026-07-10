export const STATUS_TONE_STYLES = {
  success:
    "border-emerald-500/30 bg-emerald-500/15 text-emerald-700 dark:text-emerald-300",
  warning:
    "border-amber-500/30 bg-amber-500/15 text-amber-700 dark:text-amber-300",
  danger: "border-red-500/30 bg-red-500/15 text-red-700 dark:text-red-300",
  info: "border-blue-500/30 bg-blue-500/15 text-blue-700 dark:text-blue-300",
  neutral: "border-border bg-muted text-muted-foreground",
} as const;

export type StatusTone = keyof typeof STATUS_TONE_STYLES;
