export interface ApiResponse<T> {
  code: string;
  message: string;
  result: T;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // 0-indexed
  size: number;
  first: boolean;
  last: boolean;
}

export class ApiError extends Error {
  constructor(
    public readonly code: string,
    message: string,
    public readonly status: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}
