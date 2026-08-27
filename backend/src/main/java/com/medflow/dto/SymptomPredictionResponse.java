package com.medflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Educational symptom-pattern prediction result")
public record SymptomPredictionResponse(
        @Schema(example = "VIRAL_LIKE_PATTERN")
        String predictedPattern,

        @Schema(example = "0.73")
        double confidence,

        List<PatternConfidenceResponse> alternatives,

        SymptomLikelyCauseResponse likelyCause,

        List<SymptomCauseConfidenceResponse> causeAlternatives,

        @Schema(example = "synthetic-cause-logreg-v3")
        String modelVersion,

        @Schema(example = "HIGH")
        String confidenceLevel,

        List<SymptomFactorResponse> contributingFactors,

        List<SymptomSafetyFlagResponse> safetyFlags,

        List<SymptomPossibleCauseResponse> possibleCauses,

        List<String> suggestedDoctorQuestions,

        @Schema(example = "ROUTINE_CLINICAL_REVIEW")
        String reviewPriority,

        @Schema(example = "Use this as an educational support signal along with history, examination, and clinician judgment.")
        String reviewMessage,

        @Schema(example = "Educational prediction only. Not a medical diagnosis.")
        String disclaimer,

        @Schema(description = "Encounter ID when the request was associated with an encounter", example = "1")
        Long encounterId
) {
}
