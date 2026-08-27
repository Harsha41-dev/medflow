import { apiClient } from "./client";
import type { Appointment, AppointmentRequest, AppointmentSlot, AppointmentStatus, PageResponse } from "../types";

interface AppointmentListParams {
  status?: AppointmentStatus | "";
  page: number;
  size: number;
}

export const appointmentApi = {
  async bookAppointment(request: AppointmentRequest): Promise<Appointment> {
    const response = await apiClient.post<Appointment>("/appointments", request);
    return response.data;
  },

  async listAvailableSlots(doctorId: number, date: string): Promise<AppointmentSlot[]> {
    const response = await apiClient.get<AppointmentSlot[]>("/appointments/available-slots", {
      params: { doctorId, date }
    });
    return response.data;
  },

  async listAppointments(params: AppointmentListParams): Promise<PageResponse<Appointment>> {
    const query: Record<string, string | number> = {
      page: params.page,
      size: params.size
    };

    if (params.status) {
      query.status = params.status;
    }

    const response = await apiClient.get<PageResponse<Appointment>>("/appointments", { params: query });
    return response.data;
  },

  async getAppointment(appointmentId: number): Promise<Appointment> {
    const response = await apiClient.get<Appointment>("/appointments/" + appointmentId);
    return response.data;
  },

  async cancelAppointment(appointmentId: number): Promise<Appointment> {
    const response = await apiClient.put<Appointment>("/appointments/" + appointmentId + "/cancel");
    return response.data;
  }
};
