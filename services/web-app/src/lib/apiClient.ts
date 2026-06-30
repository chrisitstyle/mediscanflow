import type { ApiError } from "@/types/apiError";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "/api/backend";

export class ApiClientError extends Error {
  status: number;
  error: string;
  path: string;
  validationErrors: Record<string, string>;

  constructor(apiError: ApiError) {
    super(apiError.message);

    this.name = "ApiClientError";
    this.status = apiError.status;
    this.error = apiError.error;
    this.path = apiError.path;
    this.validationErrors = apiError.validationErrors;
  }
}

type ApiFetchOptions = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  headers?: HeadersInit;
};

export async function apiFetch<T>(
  path: string,
  options: ApiFetchOptions = {},
): Promise<T> {
  const headers = new Headers(options.headers);

  let body: BodyInit | undefined;

  if (options.body instanceof FormData) {
    body = options.body;
  } else if (options.body !== undefined) {
    headers.set("Content-Type", "application/json");
    body = JSON.stringify(options.body);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: options.method ?? "GET",
    headers,
    body,
  });

  if (!response.ok) {
    const apiError = (await response.json()) as ApiError;
    throw new ApiClientError(apiError);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}
