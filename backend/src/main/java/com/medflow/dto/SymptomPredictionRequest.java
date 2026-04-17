package com.medflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Synthetic symptom input for educational broad pattern classification")
public record SymptomPredictionRequest(
        @Schema(example = "1")
        @NotNull(message = "Fever value is required")
        @Min(value = 0, message = "Fever must be 0 or 1")
        @Max(value = 1, message = "Fever must be 0 or 1")
        Integer fever,

        @Schema(example = "1")
        @NotNull(message = "Cough value is required")
        @Min(value = 0, message = "Cough must be 0 or 1")
        @Max(value = 1, message = "Cough must be 0 or 1")
        Integer cough,

        @Schema(example = "1")
        @NotNull(message = "Sore throat value is required")
        @Min(value = 0, message = "Sore throat must be 0 or 1")
        @Max(value = 1, message = "Sore throat must be 0 or 1")
        Integer soreThroat,

        @Schema(example = "0")
        @NotNull(message = "Runny nose value is required")
        @Min(value = 0, message = "Runny nose must be 0 or 1")
        @Max(value = 1, message = "Runny nose must be 0 or 1")
        Integer runnyNose,

        @Schema(example = "0")
        @NotNull(message = "Sneezing value is required")
        @Min(value = 0, message = "Sneezing must be 0 or 1")
        @Max(value = 1, message = "Sneezing must be 0 or 1")
        Integer sneezing,

        @Schema(example = "1")
        @NotNull(message = "Headache value is required")
        @Min(value = 0, message = "Headache must be 0 or 1")
        @Max(value = 1, message = "Headache must be 0 or 1")
        Integer headache,

        @Schema(example = "1")
        @NotNull(message = "Fatigue value is required")
        @Min(value = 0, message = "Fatigue must be 0 or 1")
        @Max(value = 1, message = "Fatigue must be 0 or 1")
        Integer fatigue,

        @Schema(example = "0")
        @NotNull(message = "Nausea value is required")
        @Min(value = 0, message = "Nausea must be 0 or 1")
        @Max(value = 1, message = "Nausea must be 0 or 1")
        Integer nausea,

        @Schema(example = "0")
        @NotNull(message = "Vomiting value is required")
        @Min(value = 0, message = "Vomiting must be 0 or 1")
        @Max(value = 1, message = "Vomiting must be 0 or 1")
        Integer vomiting,

        @Schema(example = "0")
        @NotNull(message = "Abdominal pain value is required")
        @Min(value = 0, message = "Abdominal pain must be 0 or 1")
        @Max(value = 1, message = "Abdominal pain must be 0 or 1")
        Integer abdominalPain,

        @Schema(example = "0")
        @NotNull(message = "Diarrhea value is required")
        @Min(value = 0, message = "Diarrhea must be 0 or 1")
        @Max(value = 1, message = "Diarrhea must be 0 or 1")
        Integer diarrhea,

        @Schema(example = "0")
        @NotNull(message = "Chest discomfort value is required")
        @Min(value = 0, message = "Chest discomfort must be 0 or 1")
        @Max(value = 1, message = "Chest discomfort must be 0 or 1")
        Integer chestDiscomfort,

        @Schema(example = "0")
        @NotNull(message = "Shortness of breath value is required")
        @Min(value = 0, message = "Shortness of breath must be 0 or 1")
        @Max(value = 1, message = "Shortness of breath must be 0 or 1")
        Integer shortnessOfBreath,

        @Schema(example = "1")
        @NotNull(message = "Body ache value is required")
        @Min(value = 0, message = "Body ache must be 0 or 1")
        @Max(value = 1, message = "Body ache must be 0 or 1")
        Integer bodyAche,

        @Schema(example = "0")
        @NotNull(message = "Joint pain value is required")
        @Min(value = 0, message = "Joint pain must be 0 or 1")
        @Max(value = 1, message = "Joint pain must be 0 or 1")
        Integer jointPain,

        @Schema(example = "0")
        @NotNull(message = "Dizziness value is required")
        @Min(value = 0, message = "Dizziness must be 0 or 1")
        @Max(value = 1, message = "Dizziness must be 0 or 1")
        Integer dizziness,

        @Schema(example = "0")
        @NotNull(message = "Light sensitivity value is required")
        @Min(value = 0, message = "Light sensitivity must be 0 or 1")
        @Max(value = 1, message = "Light sensitivity must be 0 or 1")
        Integer lightSensitivity,

        @Schema(example = "3")
        @NotNull(message = "Symptom duration is required")
        @Min(value = 0, message = "Symptom duration must be 0 or greater")
        Integer symptomDurationDays,

        @Schema(example = "ADULT")
        @NotNull(message = "Age group is required")
        SymptomAgeGroup ageGroup,

        @Schema(description = "Optional encounter ID for metadata-only audit logging", example = "1")
        @Positive(message = "Encounter ID must be positive")
        Long encounterId
) {
}
