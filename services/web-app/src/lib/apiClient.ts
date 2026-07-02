import { getAccessToken } from "@/lib/authTokenStore";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "/api/backend";

type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

type ApiRequestBody = BodyInit | Record<string, unknown> | null | undefined;

export type ApiFetchOptions = {
  method?: HttpMethod;
  body?: ApiRequestBody;
  headers?: HeadersInit;
};

type ApiErrorResponse = {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
  validationErrors?: Record<string, string>;
};

export class ApiClientError extends Error {
  status: number;
  error?: string;
  path?: string;
  validationErrors?: Record<string, string>;

  constructor(
    message: string,
    status: number,
    error?: string,
    path?: string,
    validationErrors?: Record<string, string>,
  ) {
    super(message);
    this.name = "ApiClientError";
    this.status = status;
    this.error = error;
    this.path = path;
    this.validationErrors = validationErrors;
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

    throw new ApiClientError(
      errorResponse.message || response.statusText || "Request failed",
      response.status,
      errorResponse.error,
      errorResponse.path,
      errorResponse.validationErrors,
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

async function parseErrorResponse(
  response: Response,
): Promise<ApiErrorResponse> {
  try {
    return (await response.json()) as ApiErrorResponse;
  } catch {
    return {
      status: response.status,
      error: response.statusText,
      message: response.statusText || "Request failed",
    };
  }
}
