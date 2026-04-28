import { apiClient } from "./client";
import type {
  Diagnosis,
  DiagnosisRequest,
  Encounter,
  EncounterRequest,
  PageResponse,
  Prescription,
  PrescriptionRequest
} from "../types";

export const clinicalApi = {
  async createEncounter(request: EncounterRequest): Promise<Encounter> {
    const response = await apiClient.post<Encounter>("/encounters", request);
    return response.data;
  },

  async getEncounter(encounterId: number): Promise<Encounter> {
    const response = await apiClient.get<Encounter>("/encounters/" + encounterId);
    return response.data;
  },

  async listPatientEncounters(patientId: number, page: number, size: number): Promise<PageResponse<Encounter>> {
    const response = await apiClient.get<PageResponse<Encounter>>("/patients/" + patientId + "/encounters", {
      params: {
        page,
        size
      }
    });
    return response.data;
  },

  async addDiagnosis(encounterId: number, request: DiagnosisRequest): Promise<Diagnosis> {
    const response = await apiClient.post<Diagnosis>("/encounters/" + encounterId + "/diagnoses", request);
    return response.data;
  },

  async addPrescription(encounterId: number, request: PrescriptionRequest): Promise<Prescription> {
    const response = await apiClient.post<Prescription>("/encounters/" + encounterId + "/prescriptions", request);
    return response.data;
  },

  async listPatientPrescriptions(patientId: number, page: number, size: number): Promise<PageResponse<Prescription>> {
    const response = await apiClient.get<PageResponse<Prescription>>("/patients/" + patientId + "/prescriptions", {
      params: {
        page,
        size
      }
    });
    return response.data;
  }
};
