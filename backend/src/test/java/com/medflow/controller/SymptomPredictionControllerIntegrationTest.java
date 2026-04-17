package com.medflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medflow.dto.PatternConfidenceResponse;
import com.medflow.dto.SymptomAgeGroup;
import com.medflow.dto.SymptomCauseConfidenceResponse;
import com.medflow.dto.SymptomFactorResponse;
import com.medflow.dto.SymptomLikelyCauseResponse;
import com.medflow.dto.SymptomModelInfoResponse;
import com.medflow.dto.SymptomPossibleCauseResponse;
import com.medflow.dto.SymptomPredictionHealthResponse;
import com.medflow.dto.SymptomPredictionRequest;
import com.medflow.dto.SymptomPredictionResponse;
import com.medflow.dto.SymptomSafetyFlagResponse;
import com.medflow.entity.Role;
import com.medflow.entity.User;
import com.medflow.exception.SymptomPredictionServiceUnavailableException;
import com.medflow.repository.AppointmentRepository;
import com.medflow.repository.AuditLogRepository;
import com.medflow.repository.DiagnosisRepository;
import com.medflow.repository.DoctorRepository;
import com.medflow.repository.EncounterRepository;
import com.medflow.repository.PatientRepository;
import com.medflow.repository.PrescriptionRepository;
import com.medflow.repository.UserRepository;
import com.medflow.security.CustomUserDetails;
import com.medflow.security.JwtService;
import com.medflow.service.SymptomPredictionService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SymptomPredictionControllerIntegrationTest {

    private static final String RAW_PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private EncounterRepository encounterRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private SymptomPredictionService symptomPredictionService;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        prescriptionRepository.deleteAll();
        diagnosisRepository.deleteAll();
        encounterRepository.deleteAll();
        appointmentRepository.deleteAll();
        doctorRepository.deleteAll();
        patientRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void doctorCanCheckSymptomPredictionHealth() throws Exception {
        User doctor = saveUser("ml-health-doctor@example.com", Role.DOCTOR);
        when(symptomPredictionService.getHealth())
                .thenReturn(new SymptomPredictionHealthResponse(
                        "UP",
                        "Symptom prediction service is available"
                ));

        mockMvc.perform(get("/api/v1/ml/symptom-pattern/health")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(doctor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").value("Symptom prediction service is available"));
    }

    @Test
    void patientCannotCheckSymptomPredictionHealth() throws Exception {
        User patient = saveUser("ml-health-patient@example.com", Role.PATIENT);

        mockMvc.perform(get("/api/v1/ml/symptom-pattern/health")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient))))
                .andExpect(status().isForbidden());
    }

    @Test
    void doctorCanViewSymptomPredictionModelInfo() throws Exception {
        User doctor = saveUser("ml-info-doctor@example.com", Role.DOCTOR);
        when(symptomPredictionService.getModelInfo())
                .thenReturn(successfulModelInfo());

        mockMvc.perform(get("/api/v1/ml/symptom-pattern/model-info")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(doctor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelVersion").value("synthetic-cause-logreg-v3"))
                .andExpect(jsonPath("$.serverMode").value("FastAPI model server loading a saved pre-trained joblib artifact"))
                .andExpect(jsonPath("$.featureCount").value(19));
    }

    @Test
    void patientCannotViewSymptomPredictionModelInfo() throws Exception {
        User patient = saveUser("ml-info-patient@example.com", Role.PATIENT);

        mockMvc.perform(get("/api/v1/ml/symptom-pattern/model-info")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient))))
                .andExpect(status().isForbidden());
    }

    @Test
    void doctorCanCallSymptomPredictionEndpoint() throws Exception {
        User doctor = saveUser("ml-doctor@example.com", Role.DOCTOR);
        when(symptomPredictionService.predict(any(SymptomPredictionRequest.class), any(Authentication.class)))
                .thenReturn(successfulResponse());

        mockMvc.perform(post("/api/v1/ml/symptom-pattern")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(doctor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.predictedPattern").value("VIRAL_LIKE_PATTERN"))
                .andExpect(jsonPath("$.confidence").value(0.73))
                .andExpect(jsonPath("$.likelyCause.code").value("FLU_LIKE_VIRAL_ILLNESS"))
                .andExpect(jsonPath("$.likelyCause.title").value("Flu-like viral illness cause"))
                .andExpect(jsonPath("$.likelyCause.evidence[0]").value("Fever selected"))
                .andExpect(jsonPath("$.causeAlternatives[0].cause").value("COMMON_COLD_LIKE"))
                .andExpect(jsonPath("$.modelVersion").value("synthetic-cause-logreg-v3"))
                .andExpect(jsonPath("$.confidenceLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.contributingFactors[0].label").value("Fever"))
                .andExpect(jsonPath("$.safetyFlags[0].severity").value("REVIEW"))
                .andExpect(jsonPath("$.possibleCauses[0].title").value("Flu-like viral illness cause"))
                .andExpect(jsonPath("$.suggestedDoctorQuestions[0]").value("When did fever, fatigue, and body ache begin?"))
                .andExpect(jsonPath("$.reviewPriority").value("PRIORITY_REVIEW"))
                .andExpect(jsonPath("$.disclaimer").value(SymptomPredictionService.DISCLAIMER));
    }

    @Test
    void patientCannotCallSymptomPredictionEndpoint() throws Exception {
        User patient = saveUser("ml-patient@example.com", Role.PATIENT);

        mockMvc.perform(post("/api/v1/ml/symptom-pattern")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotCallSymptomPredictionEndpoint() throws Exception {
        User admin = saveUser("ml-admin@example.com", Role.ADMIN);

        mockMvc.perform(post("/api/v1/ml/symptom-pattern")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserReceivesUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/ml/symptom-pattern")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidRequestReceivesBadRequest() throws Exception {
        User doctor = saveUser("ml-invalid-doctor@example.com", Role.DOCTOR);
        SymptomPredictionRequest invalidRequest = new SymptomPredictionRequest(
                2,
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
                null
        );

        mockMvc.perform(post("/api/v1/ml/symptom-pattern")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(doctor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.fever").value("Fever must be 0 or 1"));
    }

    @Test
    void mlServiceFailureReturnsServiceUnavailable() throws Exception {
        User doctor = saveUser("ml-failure-doctor@example.com", Role.DOCTOR);
        when(symptomPredictionService.predict(any(SymptomPredictionRequest.class), any(Authentication.class)))
                .thenThrow(new SymptomPredictionServiceUnavailableException(
                        "Symptom prediction service is currently unavailable"
                ));

        mockMvc.perform(post("/api/v1/ml/symptom-pattern")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(doctor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Symptom prediction service is currently unavailable"));
    }

    private SymptomPredictionRequest validRequest() {
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
                null
        );
    }

    private SymptomPredictionResponse successfulResponse() {
        return new SymptomPredictionResponse(
                "VIRAL_LIKE_PATTERN",
                0.73,
                List.of(new PatternConfidenceResponse("RESPIRATORY_PATTERN", 0.21)),
                new SymptomLikelyCauseResponse(
                        "FLU_LIKE_VIRAL_ILLNESS",
                        "Flu-like viral illness cause",
                        0.81,
                        "HIGH",
                        List.of("Fever selected", "Fatigue selected"),
                        List.of("This is trained on synthetic educational data, not real patient records."),
                        List.of("Review fever duration, hydration, breathing symptoms, and risk factors.")
                ),
                List.of(new SymptomCauseConfidenceResponse("COMMON_COLD_LIKE", 0.14)),
                "synthetic-cause-logreg-v3",
                "MEDIUM",
                List.of(new SymptomFactorResponse("fever", "Fever", "Present", 1.22)),
                List.of(new SymptomSafetyFlagResponse(
                        "BREATHING_OR_CHEST_SYMPTOM",
                        "REVIEW",
                        "Breathing difficulty or chest discomfort was selected."
                )),
                List.of(new SymptomPossibleCauseResponse(
                        "Flu-like viral illness cause",
                        "Fever, fatigue, body ache, headache, and throat symptoms together strongly support this trained cause profile."
                )),
                List.of("When did fever, fatigue, and body ache begin?", "Any sick contacts or recent travel?"),
                "PRIORITY_REVIEW",
                "Review chest or breathing symptoms carefully before using this pattern as decision support.",
                SymptomPredictionService.DISCLAIMER,
                null
        );
    }

    private SymptomModelInfoResponse successfulModelInfo() {
        return new SymptomModelInfoResponse(
                "MedFlow Educational Symptom Cause Model",
                "Two scikit-learn Logistic Regression pipelines",
                "synthetic-cause-logreg-v3",
                "FastAPI model server loading a saved pre-trained joblib artifact",
                19,
                7,
                List.of("VIRAL_LIKE_PATTERN", "RESPIRATORY_PATTERN"),
                "Synthetic MedFlow cause-profile dataset generated locally for educational use (5000 rows; pattern accuracy 0.902; cause accuracy 0.883)",
                "Pattern model uses class-specific logistic regression contributions; cause model returns ranked likely-cause probabilities",
                SymptomPredictionService.DISCLAIMER
        );
    }

    private User saveUser(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(RAW_PASSWORD));
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.saveAndFlush(user);
    }

    private String tokenFor(User user) {
        return jwtService.generateToken(CustomUserDetails.from(user));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
