import { apiClient } from "./client";
import type { AuthResponse, LoginRequest, RegisterRequest } from "../types";

export const authApi = {
  async register(request: RegisterRequest): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>("/auth/register", request);
    return response.data;
  },

  async login(request: LoginRequest): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>("/auth/login", request);
    return response.data;
  }
};
