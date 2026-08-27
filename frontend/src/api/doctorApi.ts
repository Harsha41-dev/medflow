import { apiClient } from "./client";
import type { Doctor, PageResponse } from "../types";

interface DoctorListParams {
  specialization?: string;
  page: number;
  size: number;
}

export const doctorApi = {
  async listDoctors(params: DoctorListParams): Promise<PageResponse<Doctor>> {
    const query: Record<string, string | number> = {
      page: params.page,
      size: params.size
    };

    if (params.specialization && params.specialization.trim()) {
      query.specialization = params.specialization.trim();
    }

    const response = await apiClient.get<PageResponse<Doctor>>("/doctors", { params: query });
    return response.data;
  },

  async getDoctor(doctorId: number): Promise<Doctor> {
    const response = await apiClient.get<Doctor>("/doctors/" + doctorId);
    return response.data;
  }
};
