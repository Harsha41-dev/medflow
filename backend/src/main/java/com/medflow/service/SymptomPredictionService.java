package com.medflow.service;

import com.medflow.dto.PatternConfidenceResponse;
import com.medflow.dto.SymptomCauseConfidenceResponse;
import com.medflow.dto.SymptomFactorResponse;
import com.medflow.dto.SymptomLikelyCauseResponse;
import com.medflow.dto.SymptomModelInfoResponse;
import com.medflow.dto.SymptomPossibleCauseResponse;
import com.medflow.dto.SymptomPredictionHealthResponse;
import com.medflow.dto.SymptomPredictionRequest;
import com.medflow.dto.SymptomPredictionResponse;
import com.medflow.dto.SymptomSafetyFlagResponse;
import com.medflow.entity.AuditAction;
import com.medflow.entity.Encounter;
import com.medflow.exception.BadRequestException;
import com.medflow.exception.ResourceNotFoundException;
import com.medflow.exception.SymptomPredictionServiceUnavailableException;
import com.medflow.repository.EncounterRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SymptomPredictionService {

    public static final String DISCLAIMER = "Educational prediction only. Not a medical diagnosis. "
            + "Final clinical decisions must be made by a qualified clinician.";

    private final RestClient symptomPredictionRestClient;
    private final EncounterRepository encounterRepository;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;

    public SymptomPredictionHealthResponse getHealth() {
        try {
            MlHealthResponse response = symptomPredictionRestClient.get()
                    .uri("/health")
                    .retrieve()
                    .body(MlHealthResponse.class);

            if (response != null && "UP".equalsIgnoreCase(response.status())) {
                return new SymptomPredictionHealthResponse(
                        "UP",
                        "Symptom prediction service is available"
                );
            }

            return new SymptomPredictionHealthResponse(
                    "DOWN",
                    "Symptom prediction service returned an unexpected health response"
            );
        } catch (RestClientException ex) {
            return new SymptomPredictionHealthResponse(
                    "DOWN",
                    "Symptom prediction service is currently unavailable"
            );
        }
    }

    public SymptomModelInfoResponse getModelInfo() {
        try {
            MlSymptomModelInfoResponse response = symptomPredictionRestClient.get()
                    .uri("/model-info")
                    .retrieve()
                    .body(MlSymptomModelInfoResponse.class);

            if (response == null || response.modelVersion() == null || response.modelVersion().isBlank()) {
                throw new SymptomPredictionServiceUnavailableException(
                        "Symptom prediction service returned invalid model metadata"
                );
            }

            return new SymptomModelInfoResponse(
                    response.modelName(),
                    response.modelType(),
                    response.modelVersion(),
                    response.serverMode(),
                    response.featureCount(),
                    response.patternCount(),
                    response.supportedPatterns() == null ? List.of() : response.supportedPatterns(),
                    response.trainingData(),
                    response.explanationMethod(),
                    response.disclaimer()
            );
        } catch (RestClientException ex) {
            throw unavailable(ex);
        }
    }

    @Transactional
    public SymptomPredictionResponse predict(SymptomPredictionRequest request, Authentication authentication) {
        Long auditEntityId = getAuditEntityId(request, authentication);
        String auditEntityType = request.encounterId() == null ? "SymptomPatternPrediction" : "Encounter";

        MlSymptomPredictionResponse mlResponse = callMlService(request);

        auditService.record(
                authentication,
                AuditAction.SYMPTOM_PATTERN_PREDICTED,
                auditEntityType,
                auditEntityId
        );

        return new SymptomPredictionResponse(
                mlResponse.predictedPattern(),
                valueOrZero(mlResponse.confidence()),
                mapAlternatives(mlResponse.alternatives()),
                mapLikelyCause(mlResponse.likelyCause()),
                mapCauseAlternatives(mlResponse.causeAlternatives()),
                safeText(mlResponse.modelVersion(), "unknown-model"),
                safeText(mlResponse.confidenceLevel(), "LOW"),
                mapFactors(mlResponse.contributingFactors()),
                mapSafetyFlags(mlResponse.safetyFlags()),
                mapPossibleCauses(mlResponse.possibleCauses()),
                mlResponse.suggestedDoctorQuestions() == null ? List.of() : mlResponse.suggestedDoctorQuestions(),
                safeText(mlResponse.reviewPriority(), "NEEDS_MORE_INFORMATION"),
                safeText(mlResponse.reviewMessage(), "Collect more clinical details before interpreting this educational model result."),
                DISCLAIMER,
                request.encounterId()
        );
    }

    private Long getAuditEntityId(SymptomPredictionRequest request, Authentication authentication) {
        if (request.encounterId() == null) {
            return authorizationService.getCurrentUserId(authentication);
        }

        Encounter encounter = encounterRepository.findById(request.encounterId())
                .orElseThrow(() -> new ResourceNotFoundException("Encounter not found"));
        authorizationService.checkEncounterDoctorAccess(encounter, authentication);
        return encounter.getId();
    }

    private MlSymptomPredictionResponse callMlService(SymptomPredictionRequest request) {
        try {
            MlSymptomPredictionResponse response = symptomPredictionRestClient.post()
                    .uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(toMlRequest(request))
                    .retrieve()
                    .body(MlSymptomPredictionResponse.class);

            if (response == null || response.predictedPattern() == null || response.predictedPattern().isBlank()) {
                throw new SymptomPredictionServiceUnavailableException(
                        "Symptom prediction service returned an invalid response"
                );
            }

            return response;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                throw new BadRequestException("Symptom prediction request is invalid");
            }
            throw unavailable(ex);
        } catch (RestClientException ex) {
            throw unavailable(ex);
        }
    }

    private Map<String, Object> toMlRequest(SymptomPredictionRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fever", request.fever());
        body.put("cough", request.cough());
        body.put("soreThroat", request.soreThroat());
        body.put("runnyNose", request.runnyNose());
        body.put("sneezing", request.sneezing());
        body.put("headache", request.headache());
        body.put("fatigue", request.fatigue());
        body.put("nausea", request.nausea());
        body.put("vomiting", request.vomiting());
        body.put("abdominalPain", request.abdominalPain());
        body.put("diarrhea", request.diarrhea());
        body.put("chestDiscomfort", request.chestDiscomfort());
        body.put("shortnessOfBreath", request.shortnessOfBreath());
        body.put("bodyAche", request.bodyAche());
        body.put("jointPain", request.jointPain());
        body.put("dizziness", request.dizziness());
        body.put("lightSensitivity", request.lightSensitivity());
        body.put("symptomDurationDays", request.symptomDurationDays());
        body.put("ageGroup", request.ageGroup().name());
        return body;
    }

    private List<PatternConfidenceResponse> mapAlternatives(List<MlPatternConfidenceResponse> alternatives) {
        if (alternatives == null) {
            return List.of();
        }

        return alternatives.stream()
                .map(alternative -> new PatternConfidenceResponse(
                        alternative.pattern(),
                        valueOrZero(alternative.confidence())
                ))
                .toList();
    }

    private List<SymptomFactorResponse> mapFactors(List<MlSymptomFactorResponse> factors) {
        if (factors == null) {
            return List.of();
        }

        return factors.stream()
                .map(factor -> new SymptomFactorResponse(
                        factor.field(),
                        factor.label(),
                        factor.value(),
                        valueOrZero(factor.contribution())
                ))
                .toList();
    }

    private List<SymptomSafetyFlagResponse> mapSafetyFlags(List<MlSafetyFlagResponse> safetyFlags) {
        if (safetyFlags == null) {
            return List.of();
        }

        return safetyFlags.stream()
                .map(flag -> new SymptomSafetyFlagResponse(
                        flag.code(),
                        flag.severity(),
                        flag.message()
                ))
                .toList();
    }

    private List<SymptomPossibleCauseResponse> mapPossibleCauses(List<MlPossibleCauseResponse> possibleCauses) {
        if (possibleCauses == null) {
            return List.of();
        }

        return possibleCauses.stream()
                .map(cause -> new SymptomPossibleCauseResponse(
                        cause.title(),
                        cause.reason()
                ))
                .toList();
    }

    private SymptomLikelyCauseResponse mapLikelyCause(MlLikelyCauseResponse likelyCause) {
        if (likelyCause == null) {
            return new SymptomLikelyCauseResponse(
                    "UNKNOWN",
                    "More clinical information needed",
                    0.0,
                    "LOW",
                    List.of(),
                    List.of("The ML service did not return a likely-cause explanation."),
                    List.of("Review the symptoms, history, vitals, and examination findings.")
            );
        }

        return new SymptomLikelyCauseResponse(
                safeText(likelyCause.code(), "UNKNOWN"),
                safeText(likelyCause.title(), "More clinical information needed"),
                valueOrZero(likelyCause.confidence()),
                safeText(likelyCause.confidenceLevel(), "LOW"),
                likelyCause.evidence() == null ? List.of() : likelyCause.evidence(),
                likelyCause.uncertaintyNotes() == null ? List.of() : likelyCause.uncertaintyNotes(),
                likelyCause.nextSteps() == null ? List.of() : likelyCause.nextSteps()
        );
    }

    private List<SymptomCauseConfidenceResponse> mapCauseAlternatives(List<MlCauseConfidenceResponse> alternatives) {
        if (alternatives == null) {
            return List.of();
        }

        return alternatives.stream()
                .map(alternative -> new SymptomCauseConfidenceResponse(
                        alternative.cause(),
                        valueOrZero(alternative.confidence())
                ))
                .toList();
    }

    private double valueOrZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private SymptomPredictionServiceUnavailableException unavailable(Exception ex) {
        return new SymptomPredictionServiceUnavailableException(
                "Symptom prediction service is currently unavailable",
                ex
        );
    }

    private record MlPatternConfidenceResponse(
            String pattern,
            Double confidence
    ) {
    }

    private record MlSymptomPredictionResponse(
            String predictedPattern,
            Double confidence,
            List<MlPatternConfidenceResponse> alternatives,
            MlLikelyCauseResponse likelyCause,
            List<MlCauseConfidenceResponse> causeAlternatives,
            String modelVersion,
            String confidenceLevel,
            List<MlSymptomFactorResponse> contributingFactors,
            List<MlSafetyFlagResponse> safetyFlags,
            List<MlPossibleCauseResponse> possibleCauses,
            List<String> suggestedDoctorQuestions,
            String reviewPriority,
            String reviewMessage,
            String disclaimer
    ) {
    }

    private record MlCauseConfidenceResponse(
            String cause,
            Double confidence
    ) {
    }

    private record MlLikelyCauseResponse(
            String code,
            String title,
            Double confidence,
            String confidenceLevel,
            List<String> evidence,
            List<String> uncertaintyNotes,
            List<String> nextSteps
    ) {
    }

    private record MlSymptomFactorResponse(
            String field,
            String label,
            String value,
            Double contribution
    ) {
    }

    private record MlSafetyFlagResponse(
            String code,
            String severity,
            String message
    ) {
    }

    private record MlPossibleCauseResponse(
            String title,
            String reason
    ) {
    }

    private record MlSymptomModelInfoResponse(
            String modelName,
            String modelType,
            String modelVersion,
            String serverMode,
            int featureCount,
            int patternCount,
            List<String> supportedPatterns,
            String trainingData,
            String explanationMethod,
            String disclaimer
    ) {
    }

    private record MlHealthResponse(
            String status
    ) {
    }
}
