package com.medflow.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medflow.dto.AppointmentRequest;
import com.medflow.dto.RegisterRequest;
import com.medflow.entity.Appointment;
import com.medflow.entity.AppointmentStatus;
import com.medflow.entity.Doctor;
import com.medflow.entity.Gender;
import com.medflow.entity.Patient;
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
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ValidationExceptionHandlingIntegrationTest.FailingTestController.class)
class ValidationExceptionHandlingIntegrationTest {

    private static final String RAW_PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogRepository auditLogRepository;

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
    private PatientRepository patientRepository;

    @Autowired
    private UserRepository userRepository;

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
    void blankRegistrationEmailReturnsBadRequest() throws Exception {
        RegisterRequest request = validRegisterRequest("");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register"))
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void invalidRegistrationEmailReturnsBadRequest() throws Exception {
        RegisterRequest request = validRegisterRequest("not-an-email");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void shortRegistrationPasswordReturnsBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "short-password@example.com",
                "short",
                "Asha",
                "Patel",
                LocalDate.of(2001, 5, 15),
                Gender.FEMALE,
                "9876543210",
                "Synthetic patient address",
                "9876500000"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void futureDateOfBirthReturnsBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "future-dob@example.com",
                RAW_PASSWORD,
                "Asha",
                "Patel",
                LocalDate.now().plusDays(1),
                Gender.FEMALE,
                "9876543210",
                "Synthetic patient address",
                "9876500000"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.dateOfBirth").exists());
    }

    @Test
    void missingDoctorIdDuringBookingReturnsBadRequest() throws Exception {
        Patient patient = savePatient("missing-doctor@example.com", "Aarav", "Sharma");
        Map<String, Object> payload = Map.of(
                "appointmentDateTime", futureSlot(7, 10, 0).toString(),
                "reason", "Skin irritation"
        );

        mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.doctorId").exists());
    }

    @Test
    void pastAppointmentTimeReturnsBadRequest() throws Exception {
        Patient patient = savePatient("past-time@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("past-time-doctor@example.com", "Ananya", "Rao", "Dermatology", "VAL100", true);
        AppointmentRequest request = new AppointmentRequest(doctor.getId(), pastSlot(), "Skin irritation");

        mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.appointmentDateTime").exists());
    }

    @Test
    void invalidAppointmentSlotReturnsBadRequest() throws Exception {
        Patient patient = savePatient("invalid-slot@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("invalid-slot-doctor@example.com", "Ananya", "Rao", "Dermatology", "VAL101", true);
        AppointmentRequest request = new AppointmentRequest(doctor.getId(), futureSlot(7, 10, 10), "Skin irritation");

        mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Appointments must use 30-minute slots"));
    }

    @Test
    void appointmentConflictReturnsConflict() throws Exception {
        Patient patientA = savePatient("conflict-a@example.com", "Aarav", "Sharma");
        Patient patientB = savePatient("conflict-b@example.com", "Isha", "Mehta");
        Doctor doctor = saveDoctor("conflict-doctor@example.com", "Ananya", "Rao", "Dermatology", "VAL102", true);
        LocalDateTime appointmentTime = futureSlot(7, 10, 0);
        saveAppointment(patientA, doctor, appointmentTime, AppointmentStatus.SCHEDULED);
        AppointmentRequest request = new AppointmentRequest(doctor.getId(), appointmentTime, "Follow up");

        mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patientB.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Doctor already has a scheduled appointment at this time"));
    }

    @Test
    void missingEncounterChiefComplaintReturnsBadRequest() throws Exception {
        Doctor doctor = saveDoctor("encounter-validation-doctor@example.com", "Ananya", "Rao", "Dermatology", "VAL103", true);
        Map<String, Object> payload = Map.of("appointmentId", 1L);

        mockMvc.perform(post("/api/v1/encounters")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(doctor.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.chiefComplaint").exists());
    }

    @Test
    void missingDiagnosisCodeAndNameReturnsBadRequest() throws Exception {
        Doctor doctor = saveDoctor("diagnosis-validation-doctor@example.com", "Ananya", "Rao", "Dermatology", "VAL104", true);
        Map<String, Object> payload = Map.of("description", "Optional description");

        mockMvc.perform(post("/api/v1/encounters/1/diagnoses")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(doctor.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.diagnosisCode").exists())
                .andExpect(jsonPath("$.fieldErrors.diagnosisName").exists());
    }

    @Test
    void missingPrescriptionMedicationAndDosageReturnsBadRequest() throws Exception {
        Doctor doctor = saveDoctor("prescription-validation-doctor@example.com", "Ananya", "Rao", "Dermatology", "VAL105", true);
        Map<String, Object> payload = Map.of(
                "frequency", "Once daily",
                "duration", "5 days"
        );

        mockMvc.perform(post("/api/v1/encounters/1/prescriptions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(doctor.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.medicationName").exists())
                .andExpect(jsonPath("$.fieldErrors.dosage").exists());
    }

    @Test
    void resourceNotFoundReturnsNotFound() throws Exception {
        Patient patient = savePatient("not-found-patient@example.com", "Aarav", "Sharma");

        mockMvc.perform(get("/api/v1/appointments/99999")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Appointment not found"));
    }

    @Test
    void unauthorizedResourceAccessReturnsForbidden() throws Exception {
        Patient patientA = savePatient("forbidden-a@example.com", "Aarav", "Sharma");
        Patient patientB = savePatient("forbidden-b@example.com", "Isha", "Mehta");

        mockMvc.perform(get("/api/v1/patients/{id}", patientB.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patientA.getUser()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You are not allowed to access this patient profile"));
    }

    @Test
    void missingAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/appointments/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Unauthorized request"));
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        String payload = """
                {
                  "email": "broken@example.com",
                  "password":
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void invalidJsonTypeReturnsBadRequest() throws Exception {
        Patient patient = savePatient("invalid-type@example.com", "Aarav", "Sharma");
        String payload = """
                {
                  "doctorId": "abc",
                  "appointmentDateTime": "2030-01-01T10:00:00",
                  "reason": "Skin irritation"
                }
                """;

        mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void invalidEnumValueReturnsBadRequest() throws Exception {
        String payload = """
                {
                  "email": "invalid-enum@example.com",
                  "password": "Password123!",
                  "firstName": "Asha",
                  "lastName": "Patel",
                  "dateOfBirth": "2001-05-15",
                  "gender": "UNKNOWN_VALUE",
                  "phone": "9876543210",
                  "address": "Synthetic patient address",
                  "emergencyContact": "9876500000"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid enum value in request"));
    }

    @Test
    void invalidEnumQueryParameterReturnsBadRequest() throws Exception {
        Patient patient = savePatient("invalid-query@example.com", "Aarav", "Sharma");

        mockMvc.perform(get("/api/v1/appointments?status=INVALID")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid enum value for parameter: status"));
    }

    @Test
    void unexpectedExceptionReturnsSafeServerError() throws Exception {
        User admin = saveUser("unexpected-admin@example.com", Role.ADMIN, true);

        MvcResult result = mockMvc.perform(get("/api/v1/test/unexpected-error")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(admin))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.path").value("/api/v1/test/unexpected-error"))
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody)
                .doesNotContain("IllegalStateException")
                .doesNotContain("Synthetic unexpected failure")
                .doesNotContain("java.")
                .doesNotContain(".java")
                .doesNotContain("SQL")
                .doesNotContain("D:\\");
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

    private Appointment saveAppointment(
            Patient patient,
            Doctor doctor,
            LocalDateTime appointmentTime,
            AppointmentStatus status
    ) {
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDateTime(appointmentTime);
        appointment.setStatus(status);
        appointment.setReason("Synthetic appointment reason");
        return appointmentRepository.saveAndFlush(appointment);
    }

    private Patient savePatient(String email, String firstName, String lastName) {
        User user = saveUser(email, Role.PATIENT, true);

        Patient patient = new Patient();
        patient.setUser(user);
        patient.setFirstName(firstName);
        patient.setLastName(lastName);
        patient.setDateOfBirth(LocalDate.of(2002, 5, 14));
        patient.setGender(Gender.MALE);
        patient.setPhone("9876543210");
        patient.setAddress("Synthetic patient address");
        patient.setEmergencyContact("9876543211");
        return patientRepository.saveAndFlush(patient);
    }

    private Doctor saveDoctor(
            String email,
            String firstName,
            String lastName,
            String specialization,
            String licenseNumber,
            boolean active
    ) {
        User user = saveUser(email, Role.DOCTOR, true);

        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setFirstName(firstName);
        doctor.setLastName(lastName);
        doctor.setSpecialization(specialization);
        doctor.setLicenseNumber(licenseNumber);
        doctor.setActive(active);
        return doctorRepository.saveAndFlush(doctor);
    }

    private User saveUser(String email, Role role, boolean enabled) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(RAW_PASSWORD));
        user.setRole(role);
        user.setEnabled(enabled);
        return userRepository.saveAndFlush(user);
    }

    private String tokenFor(User user) {
        return jwtService.generateToken(CustomUserDetails.from(user));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private LocalDateTime futureSlot(int daysFromNow, int hour, int minute) {
        return LocalDateTime.now()
                .plusDays(daysFromNow)
                .withHour(hour)
                .withMinute(minute)
                .withSecond(0)
                .withNano(0);
    }

    private LocalDateTime pastSlot() {
        return LocalDateTime.now()
                .minusDays(1)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }

    @RestController
    static class FailingTestController {

        @GetMapping("/api/v1/test/unexpected-error")
        public Map<String, String> unexpectedError() {
            throw new IllegalStateException("Synthetic unexpected failure");
        }
    }
}
