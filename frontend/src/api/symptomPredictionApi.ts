import { apiClient } from "./client";
import type { SymptomModelInfo, SymptomPrediction, SymptomPredictionHealth, SymptomPredictionRequest } from "../types";

export const symptomPredictionApi = {
  async getHealth(): Promise<SymptomPredictionHealth> {
    const response = await apiClient.get<SymptomPredictionHealth>("/ml/symptom-pattern/health");
    return response.data;
  },

  async getModelInfo(): Promise<SymptomModelInfo> {
    const response = await apiClient.get<SymptomModelInfo>("/ml/symptom-pattern/model-info");
    return response.data;
  },

  async predict(request: SymptomPredictionRequest): Promise<SymptomPrediction> {
    const response = await apiClient.post<SymptomPrediction>("/ml/symptom-pattern", request);
    return response.data;
  }
};
