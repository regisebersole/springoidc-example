export interface ApiConfig {
  baseUrl: string;
  timeout: number;
  retryAttempts: number;
}

export interface ErrorResponse {
  error: string;
  message: string;
  timestamp: string;
}

export interface ApiResponse<T = unknown> {
  data: T;
  status: number;
  statusText: string;
}