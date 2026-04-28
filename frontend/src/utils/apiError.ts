import axios from "axios";
import type { ApiErrorResponse } from "../types";

export function getApiErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    const response = error.response;
    const data = response?.data;

    if (data?.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
      return Object.values(data.fieldErrors).join(" ");
    }

    if (data?.message) {
      return data.message;
    }

    if (response?.status === 401) {
      return "Please login again.";
    }

    if (response?.status === 403) {
      return "You do not have permission to perform this action.";
    }
  }

  return "Something went wrong. Please try again.";
}
