import { apiClient } from "./client";
import type { Patient, PatientUpdateRequest } from "../types";

export const patientApi = {
  async getPatient(patientId: number): Promise<Patient> {
    const response = await apiClient.get<Patient>("/patients/" + patientId);
    return response.data;
  },

  async updatePatient(patientId: number, request: PatientUpdateRequest): Promise<Patient> {
    const response = await apiClient.put<Patient>("/patients/" + patientId, request);
    return response.data;
  }
};
