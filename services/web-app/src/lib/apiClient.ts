import { getAccessToken } from "@/lib/authTokenStore";
import type { ApiError, ApiValidationErrors } from "@/types/apiError";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "/api/backend";

type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

type ApiRequestBody = BodyInit | Record<string, unknown> | null | undefined;

export type ApiFetchOptions = {
  method?: HttpMethod;
  body?: ApiRequestBody;
  headers?: HeadersInit;
};

export class ApiClientError extends Error {
  status: number;
  error?: string;
  path?: string;
  validationErrors?: ApiValidationErrors;
  response: ApiError;

  constructor(response: ApiError) {
    super(response.message || response.error || "Request failed");

    this.name = "ApiClientError";
    this.status = response.status;
    this.error = response.error;
    this.path = response.path;
    this.validationErrors = response.validationErrors;
    this.response = response;
  }
}

export async function apiFetch<T>(
  path: string,
  options: ApiFetchOptions = {},
): Promise<T> {
  const headers = new Headers(options.headers);
  const token = getAccessToken();

  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const requestInit: RequestInit = {
    method: options.method ?? "GET",
    headers,
  };

  if (options.body !== undefined && options.body !== null) {
    if (options.body instanceof FormData) {
      requestInit.body = options.body;
    } else if (
      typeof options.body === "string" ||
      options.body instanceof Blob ||
      options.body instanceof ArrayBuffer ||
      options.body instanceof URLSearchParams
    ) {
      requestInit.body = options.body;
    } else {
      headers.set("Content-Type", "application/json");
      requestInit.body = JSON.stringify(options.body);
    }
  }

  const response = await fetch(`${API_BASE_URL}${path}`, requestInit);

  if (!response.ok) {
    const errorResponse = await parseErrorResponse(response);

    throw new ApiClientError(errorResponse);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

async function parseErrorResponse(response: Response): Promise<ApiError> {
  try {
    const errorResponse = (await response.json()) as Partial<ApiError>;

    return normalizeErrorResponse(errorResponse, response);
  } catch {
    return fallbackErrorResponse(response);
  }
}

function normalizeErrorResponse(
  errorResponse: Partial<ApiError>,
  response: Response,
): ApiError {
  return {
    timestamp: errorResponse.timestamp,
    status: errorResponse.status ?? response.status,
    error: errorResponse.error ?? response.statusText,
    message: (errorResponse.message ?? response.statusText) || "Request failed",
    path: errorResponse.path,
    validationErrors: errorResponse.validationErrors,
  };
}

function fallbackErrorResponse(response: Response): ApiError {
  return {
    status: response.status,
    error: response.statusText,
    message: response.statusText || "Request failed",
  };
}
