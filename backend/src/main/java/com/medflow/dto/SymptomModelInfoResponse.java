package com.medflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Metadata for the educational symptom-pattern and likely-cause ML model")
public record SymptomModelInfoResponse(
        @Schema(example = "MedFlow Educational Symptom Cause Model")
        String modelName,

        @Schema(example = "Two scikit-learn Logistic Regression pipelines")
        String modelType,

        @Schema(example = "synthetic-cause-logreg-v3")
        String modelVersion,

        @Schema(example = "FastAPI model server loading a saved pre-trained joblib artifact")
        String serverMode,

        @Schema(example = "19")
        int featureCount,

        @Schema(example = "7")
        int patternCount,

        List<String> supportedPatterns,

        @Schema(example = "Synthetic MedFlow cause-profile dataset generated locally for educational use (5000 rows; pattern accuracy 0.902; cause accuracy 0.883)")
        String trainingData,

        @Schema(example = "Pattern model uses class-specific logistic regression contributions; cause model returns ranked likely-cause probabilities")
        String explanationMethod,

        @Schema(example = "Educational prediction only. Not a medical diagnosis.")
        String disclaimer
) {
}
