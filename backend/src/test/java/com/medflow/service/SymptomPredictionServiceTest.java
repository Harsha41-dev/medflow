package com.medflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.medflow.dto.SymptomAgeGroup;
import com.medflow.dto.SymptomPredictionRequest;
import com.medflow.entity.AuditAction;
import com.medflow.entity.Encounter;
import com.medflow.exception.SymptomPredictionServiceUnavailableException;
import com.medflow.repository.EncounterRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SymptomPredictionServiceTest {

    private EncounterRepository encounterRepository;
    private AuthorizationService authorizationService;
    private AuditService auditService;
    private Authentication authentication;
    private MockRestServiceServer mockServer;
    private SymptomPredictionService symptomPredictionService;

    @BeforeEach
    void setUp() {
        encounterRepository = mock(EncounterRepository.class);
        authorizationService = mock(AuthorizationService.class);
        auditService = mock(AuditService.class);
        authentication = mock(Authentication.class);

        RestClient.Builder builder = RestClient.builder().baseUrl("http://ml-test");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        symptomPredictionService = new SymptomPredictionService(
                builder.build(),
                encounterRepository,
                authorizationService,
                auditService
        );
    }

    @Test
    void healthReturnsUpWhenMlServiceIsReachable() {
        mockServer.expect(requestTo("http://ml-test/health"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":\"UP\"}", MediaType.APPLICATION_JSON));

        var response = symptomPredictionService.getHealth();

        assertThat(response.status()).isEqualTo("UP");
        assertThat(response.message()).isEqualTo("Symptom prediction service is available");
        mockServer.verify();
    }

    @Test
    void healthReturnsDownWhenMlServiceIsUnavailable() {
        mockServer.expect(requestTo("http://ml-test/health"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        var response = symptomPredictionService.getHealth();

        assertThat(response.status()).isEqualTo("DOWN");
        assertThat(response.message()).isEqualTo("Symptom prediction service is currently unavailable");
        mockServer.verify();
    }

    @Test
    void modelInfoReturnsLoadedMlModelMetadata() {
        mockServer.expect(requestTo("http://ml-test/model-info"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(successfulModelInfoJson(), MediaType.APPLICATION_JSON));

        var response = symptomPredictionService.getModelInfo();

        assertThat(response.modelName()).isEqualTo("MedFlow Educational Symptom Cause Model");
        assertThat(response.modelVersion()).isEqualTo("synthetic-cause-logreg-v3");
        assertThat(response.serverMode()).contains("FastAPI model server");
        assertThat(response.featureCount()).isEqualTo(19);
        assertThat(response.supportedPatterns()).contains("VIRAL_LIKE_PATTERN");
        assertThat(response.trainingData()).contains("cause accuracy");
        mockServer.verify();
    }

    @Test
    void predictionCallsMlServiceAndRecordsMetadataAudit() {
        when(authorizationService.getCurrentUserId(authentication)).thenReturn(15L);
        mockServer.expect(requestTo("http://ml-test/predict"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(successfulPredictionJson(), MediaType.APPLICATION_JSON));

        var response = symptomPredictionService.predict(validRequest(null), authentication);

        assertThat(response.predictedPattern()).isEqualTo("VIRAL_LIKE_PATTERN");
        assertThat(response.confidence()).isEqualTo(0.73);
        assertThat(response.alternatives()).hasSize(1);
        assertThat(response.likelyCause().code()).isEqualTo("FLU_LIKE_VIRAL_ILLNESS");
        assertThat(response.likelyCause().title()).isEqualTo("Flu-like viral illness cause");
        assertThat(response.likelyCause().evidence()).contains("Fever selected");
        assertThat(response.likelyCause().uncertaintyNotes()).contains("This is trained on synthetic educational data, not real patient records.");
        assertThat(response.likelyCause().nextSteps()).contains("Review fever duration, hydration, breathing symptoms, and risk factors.");
        assertThat(response.causeAlternatives()).hasSize(1);
        assertThat(response.modelVersion()).isEqualTo("synthetic-cause-logreg-v3");
        assertThat(response.confidenceLevel()).isEqualTo("MEDIUM");
        assertThat(response.contributingFactors()).hasSize(1);
        assertThat(response.safetyFlags()).hasSize(1);
        assertThat(response.possibleCauses()).hasSize(1);
        assertThat(response.possibleCauses().get(0).title()).isEqualTo("Flu-like viral illness cause");
        assertThat(response.suggestedDoctorQuestions()).contains("When did fever, fatigue, and body ache begin?");
        assertThat(response.reviewPriority()).isEqualTo("PRIORITY_REVIEW");
        assertThat(response.disclaimer()).contains("Not a medical diagnosis");
        verify(auditService).record(
                authentication,
                AuditAction.SYMPTOM_PATTERN_PREDICTED,
                "SymptomPatternPrediction",
                15L
        );
        mockServer.verify();
    }

    @Test
    void encounterIdIsAuthorizedBeforeAuditReferenceIsRecorded() {
        Encounter encounter = new Encounter();
        encounter.setId(44L);
        when(encounterRepository.findById(44L)).thenReturn(Optional.of(encounter));
        mockServer.expect(requestTo("http://ml-test/predict"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(successfulPredictionJson(), MediaType.APPLICATION_JSON));

        var response = symptomPredictionService.predict(validRequest(44L), authentication);

        assertThat(response.encounterId()).isEqualTo(44L);
        verify(authorizationService).checkEncounterDoctorAccess(encounter, authentication);
        verify(auditService).record(
                authentication,
                AuditAction.SYMPTOM_PATTERN_PREDICTED,
                "Encounter",
                44L
        );
        mockServer.verify();
    }

    @Test
    void mlServiceFailureThrowsUnavailableAndDoesNotRecordSuccessAudit() {
        when(authorizationService.getCurrentUserId(authentication)).thenReturn(15L);
        mockServer.expect(requestTo("http://ml-test/predict"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> symptomPredictionService.predict(validRequest(null), authentication))
                .isInstanceOf(SymptomPredictionServiceUnavailableException.class)
                .hasMessage("Symptom prediction service is currently unavailable");

        verify(auditService, never()).record(
                authentication,
                AuditAction.SYMPTOM_PATTERN_PREDICTED,
                "SymptomPatternPrediction",
                15L
        );
        mockServer.verify();
    }

    private SymptomPredictionRequest validRequest(Long encounterId) {
        return new SymptomPredictionRequest(
                1,
                1,
                1,
                0,
                0,
                1,
                1,
                0,
                0,
                0,
                0,
                0,
                0,
                1,
                0,
                0,
                0,
                3,
                SymptomAgeGroup.ADULT,
                encounterId
        );
    }

    private String successfulPredictionJson() {
        return """
                {
                  "predictedPattern": "VIRAL_LIKE_PATTERN",
                  "confidence": 0.73,
                  "alternatives": [
                    {
                      "pattern": "RESPIRATORY_PATTERN",
                      "confidence": 0.21
                    }
                  ],
                  "likelyCause": {
                    "code": "FLU_LIKE_VIRAL_ILLNESS",
                    "title": "Flu-like viral illness cause",
                    "confidence": 0.81,
                    "confidenceLevel": "HIGH",
                    "evidence": [
                      "Fever selected",
                      "Fatigue selected"
                    ],
                    "uncertaintyNotes": [
                      "This is trained on synthetic educational data, not real patient records."
                    ],
                    "nextSteps": [
                      "Review fever duration, hydration, breathing symptoms, and risk factors."
                    ]
                  },
                  "causeAlternatives": [
                    {
                      "cause": "COMMON_COLD_LIKE",
                      "confidence": 0.14
                    }
                  ],
                  "modelVersion": "synthetic-cause-logreg-v3",
                  "confidenceLevel": "MEDIUM",
                  "contributingFactors": [
                    {
                      "field": "fever",
                      "label": "Fever",
                      "value": "Present",
                      "contribution": 1.22
                    }
                  ],
                  "safetyFlags": [
                    {
                      "code": "BREATHING_OR_CHEST_SYMPTOM",
                      "severity": "REVIEW",
                      "message": "Breathing difficulty or chest discomfort was selected."
                    }
                  ],
                  "possibleCauses": [
                    {
                      "title": "Flu-like viral illness cause",
                      "reason": "Fever, fatigue, body ache, headache, and throat symptoms together strongly support this trained cause profile."
                    }
                  ],
                  "suggestedDoctorQuestions": [
                    "When did fever, fatigue, and body ache begin?",
                    "Any sick contacts or recent travel?"
                  ],
                  "reviewPriority": "PRIORITY_REVIEW",
                  "reviewMessage": "Review chest or breathing symptoms carefully before using this pattern as decision support.",
                  "disclaimer": "Educational prediction only. Not a medical diagnosis."
                }
                """;
    }

    private String successfulModelInfoJson() {
        return """
                {
                  "modelName": "MedFlow Educational Symptom Cause Model",
                  "modelType": "Two scikit-learn Logistic Regression pipelines",
                  "modelVersion": "synthetic-cause-logreg-v3",
                  "serverMode": "FastAPI model server loading a saved pre-trained joblib artifact",
                  "featureCount": 19,
                  "patternCount": 7,
                  "supportedPatterns": [
                    "VIRAL_LIKE_PATTERN",
                    "RESPIRATORY_PATTERN"
                  ],
                  "trainingData": "Synthetic MedFlow cause-profile dataset generated locally for educational use (5000 rows; pattern accuracy 0.902; cause accuracy 0.883)",
                  "explanationMethod": "Pattern model uses class-specific logistic regression contributions; cause model returns ranked likely-cause probabilities",
                  "disclaimer": "Educational prediction only. Not a medical diagnosis."
                }
                """;
    }
}
