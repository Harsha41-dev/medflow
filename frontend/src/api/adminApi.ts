import { apiClient } from "./client";
import type { AuditAction, AuditLog, CreateDoctorRequest, Doctor, PageResponse } from "../types";

interface AuditLogParams {
  action?: AuditAction | "";
  userId?: string;
  page: number;
  size: number;
}

export const adminApi = {
  async createDoctor(request: CreateDoctorRequest): Promise<Doctor> {
    const response = await apiClient.post<Doctor>("/admin/doctors", request);
    return response.data;
  },

  async listAuditLogs(params: AuditLogParams): Promise<PageResponse<AuditLog>> {
    const query: Record<string, string | number> = {
      page: params.page,
      size: params.size
    };

    if (params.action) {
      query.action = params.action;
    }

    if (params.userId && params.userId.trim()) {
      query.userId = params.userId.trim();
    }

    const response = await apiClient.get<PageResponse<AuditLog>>("/admin/audit-logs", { params: query });
    return response.data;
  }
};
