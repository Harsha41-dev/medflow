package com.medflow.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medflow.dto.LoginRequest;
import com.medflow.dto.RegisterRequest;
import com.medflow.entity.Gender;
import com.medflow.entity.Role;
import com.medflow.entity.User;
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
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthControllerIntegrationTest.ProtectedTestController.class)
class AuthControllerIntegrationTest {

    private static final String RAW_PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private EncounterRepository encounterRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

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
    void patientRegistrationAssignsPatientRole() throws Exception {
        RegisterRequest request = validRegisterRequest("patient@example.com");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.profileId").isNumber())
                .andExpect(jsonPath("$.email").value("patient@example.com"))
                .andExpect(jsonPath("$.role").value("PATIENT"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        User savedUser = userRepository.findByEmail("patient@example.com").orElseThrow();
        assertThat(savedUser.getRole()).isEqualTo(Role.PATIENT);
        assertThat(patientRepository.findByUserId(savedUser.getId())).isPresent();
    }

    @Test
    void patientRegistrationStoresEncodedPassword() throws Exception {
        RegisterRequest request = validRegisterRequest("encoded@example.com");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        User savedUser = userRepository.findByEmail("encoded@example.com").orElseThrow();
        assertThat(savedUser.getPassword()).isNotEqualTo(RAW_PASSWORD);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, savedUser.getPassword())).isTrue();
    }

    @Test
    void duplicateEmailRegistrationFails() throws Exception {
        RegisterRequest request = validRegisterRequest("duplicate@example.com");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void registrationWithRoleFieldIsRejected() throws Exception {
        String payload = """
                {
                  "email": "role-attempt@example.com",
                  "password": "Password123!",
                  "firstName": "Role",
                  "lastName": "Attempt",
                  "dateOfBirth": "2001-05-15",
                  "gender": "OTHER",
                  "phone": "9876543210",
                  "address": "Synthetic address",
                  "emergencyContact": "9876500000",
                  "role": "ADMIN"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.existsByEmail("role-attempt@example.com")).isFalse();
    }

    @Test
    void validLoginSucceeds() throws Exception {
        saveUser("login@example.com", RAW_PASSWORD, Role.PATIENT, true);
        LoginRequest request = new LoginRequest("login@example.com", RAW_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value("login@example.com"))
                .andExpect(jsonPath("$.role").value("PATIENT"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void wrongPasswordFails() throws Exception {
        saveUser("wrong-password@example.com", RAW_PASSWORD, Role.PATIENT, true);
        LoginRequest request = new LoginRequest("wrong-password@example.com", "WrongPassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void protectedEndpointRejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/v1/test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized request"));
    }

    @Test
    void validJwtAuthenticatesRequest() throws Exception {
        User user = saveUser("jwt@example.com", RAW_PASSWORD, Role.PATIENT, true);
        String token = jwtService.generateToken(CustomUserDetails.from(user));

        mockMvc.perform(get("/api/v1/test/protected")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jwt@example.com"));
    }

    @Test
    void invalidJwtReturnsUnauthorizedResponse() throws Exception {
        mockMvc.perform(get("/api/v1/test/protected")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired JWT token"));
    }

    @Test
    void patientJwtCannotAccessAdminEndpoint() throws Exception {
        User user = saveUser("patient-admin-check@example.com", RAW_PASSWORD, Role.PATIENT, true);
        String token = jwtService.generateToken(CustomUserDetails.from(user));

        mockMvc.perform(get("/api/v1/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Forbidden request"));
    }

    @Test
    void disabledUserCannotAuthenticate() throws Exception {
        saveUser("disabled@example.com", RAW_PASSWORD, Role.PATIENT, false);
        LoginRequest request = new LoginRequest("disabled@example.com", RAW_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User account is disabled"));
    }

    private RegisterRequest validRegisterRequest(String email) {
        return new RegisterRequest(
                email,
                RAW_PASSWORD,
                "Asha",
                "Patel",
                LocalDate.of(2001, 5, 15),
                Gender.FEMALE,
                "9876543210",
                "Synthetic patient address",
                "9876500000"
        );
    }

    private User saveUser(String email, String rawPassword, Role role, boolean enabled) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setEnabled(enabled);
        return userRepository.saveAndFlush(user);
    }

    @RestController
    public static class ProtectedTestController {

        @GetMapping("/api/v1/test/protected")
        public Map<String, String> protectedEndpoint(Authentication authentication) {
            return Map.of("email", authentication.getName());
        }
    }
}
