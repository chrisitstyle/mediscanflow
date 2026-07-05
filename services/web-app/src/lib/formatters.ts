const EMPTY_VALUE = "—";

const dateFormatter = new Intl.DateTimeFormat("en", {
  dateStyle: "medium",
});

const dateTimeFormatter = new Intl.DateTimeFormat("en", {
  dateStyle: "medium",
  timeStyle: "short",
});

/**
 * Formats an ISO date string for display in the UI.
 * Returns an em dash when the value is missing.
 */
export function formatDate(value: string | null | undefined): string {
  if (!value) {
    return EMPTY_VALUE;
  }

  return dateFormatter.format(new Date(value));
}

/**
 * Formats an ISO date-time string for display in the UI.
 * Returns an em dash when the value is missing.
 */
export function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return EMPTY_VALUE;
  }

  return dateTimeFormatter.format(new Date(value));
}

/**
 * Formats a file size in bytes into a compact human-readable value.
 */
export function formatFileSize(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }

  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }

  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}
