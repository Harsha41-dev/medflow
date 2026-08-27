import axios, { type AxiosError, type InternalAxiosRequestConfig } from "axios";
import type { ApiErrorResponse } from "../types";
import { clearStoredAuth, getStoredAuth } from "../utils/authStorage";

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api/v1";

export const apiClient = axios.create({
  baseURL: apiBaseUrl,
  headers: {
    "Content-Type": "application/json"
  }
});

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const auth = getStoredAuth();
  if (auth?.accessToken) {
    config.headers.Authorization = "Bearer " + auth.accessToken;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorResponse>) => {
    if (error.response?.status === 401) {
      clearStoredAuth();
      const path = window.location.pathname;
      if (path !== "/login" && path !== "/register") {
        window.location.assign("/login");
      }
    }
    return Promise.reject(error);
  }
);
