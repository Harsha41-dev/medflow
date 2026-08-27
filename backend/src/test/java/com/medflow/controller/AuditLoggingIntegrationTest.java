package com.medflow.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medflow.dto.AppointmentRequest;
import com.medflow.dto.CreateDoctorRequest;
import com.medflow.dto.DiagnosisRequest;
import com.medflow.dto.EncounterRequest;
import com.medflow.dto.LoginRequest;
import com.medflow.dto.PatientUpdateRequest;
import com.medflow.dto.PrescriptionRequest;
import com.medflow.entity.Appointment;
import com.medflow.entity.AppointmentStatus;
import com.medflow.entity.AuditAction;
import com.medflow.entity.AuditLog;
import com.medflow.entity.Doctor;
import com.medflow.entity.Encounter;
import com.medflow.entity.Gender;
import com.medflow.entity.Patient;
import com.medflow.entity.Prescription;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditLoggingIntegrationTest {

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
    void successfulLoginCreatesAuditEntry() throws Exception {
        User user = saveUser("login-audit@example.com", Role.PATIENT, true);
        LoginRequest request = new LoginRequest("login-audit@example.com", RAW_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        AuditLog auditLog = latestAuditLog(AuditAction.USER_LOGIN_SUCCESS);
        assertThat(auditLog.getUser().getId()).isEqualTo(user.getId());
        assertThat(auditLog.getEntityType()).isEqualTo("User");
        assertThat(auditLog.getEntityId()).isEqualTo(user.getId());
    }

    @Test
    void failedLoginCreatesAuditEntryForExistingUser() throws Exception {
        User user = saveUser("failed-login-audit@example.com", Role.PATIENT, true);
        LoginRequest request = new LoginRequest("failed-login-audit@example.com", "WrongPassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        AuditLog auditLog = latestAuditLog(AuditAction.USER_LOGIN_FAILED);
        assertThat(auditLog.getUser().getId()).isEqualTo(user.getId());
        assertThat(auditLog.getEntityType()).isEqualTo("User");
        assertThat(auditLog.getEntityId()).isEqualTo(user.getId());
    }

    @Test
    void successfulAppointmentBookingCreatesAuditEntry() throws Exception {
        Patient patient = savePatient("booking-audit@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("booking-doctor-audit@example.com", "Ananya", "Rao", "Dermatology", "AUD100", true);
        AppointmentRequest request = new AppointmentRequest(doctor.getId(), futureSlot(7, 10, 0), "Skin irritation");

        mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        AuditLog auditLog = latestAuditLog(AuditAction.APPOINTMENT_CREATED);
        assertThat(auditLog.getUser().getId()).isEqualTo(patient.getUser().getId());
        assertThat(auditLog.getEntityType()).isEqualTo("Appointment");
        assertThat(auditLog.getEntityId()).isNotNull();
    }

    @Test
    void appointmentViewCreatesAuditEntry() throws Exception {
        Patient patient = savePatient("appointment-view-audit@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor(
                "appointment-view-doctor-audit@example.com",
                "Ananya",
                "Rao",
                "Dermatology",
                "AUD108",
                true
        );
        Appointment appointment = saveAppointment(patient, doctor, AppointmentStatus.SCHEDULED);

        mockMvc.perform(get("/api/v1/appointments/{id}", appointment.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser()))))
                .andExpect(status().isOk());

        AuditLog auditLog = latestAuditLog(AuditAction.APPOINTMENT_VIEWED);
        assertThat(auditLog.getUser().getId()).isEqualTo(patient.getUser().getId());
        assertThat(auditLog.getEntityType()).isEqualTo("Appointment");
        assertThat(auditLog.getEntityId()).isEqualTo(appointment.getId());
    }

    @Test
    void appointmentCancellationCreatesAuditEntry() throws Exception {
        Patient patient = savePatient("cancel-audit@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("cancel-doctor-audit@example.com", "Ananya", "Rao", "Dermatology", "AUD101", true);
        Appointment appointment = saveAppointment(patient, doctor, AppointmentStatus.SCHEDULED);

        mockMvc.perform(put("/api/v1/appointments/{id}/cancel", appointment.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser()))))
                .andExpect(status().isOk());

        AuditLog auditLog = latestAuditLog(AuditAction.APPOINTMENT_CANCELLED);
        assertThat(auditLog.getUser().getId()).isEqualTo(patient.getUser().getId());
        assertThat(auditLog.getEntityType()).isEqualTo("Appointment");
        assertThat(auditLog.getEntityId()).isEqualTo(appointment.getId());
    }

    @Test
    void patientUpdateCreatesAuditEntry() throws Exception {
        Patient patient = savePatient("patient-update-audit@example.com", "Aarav", "Sharma");
        PatientUpdateRequest request = new PatientUpdateRequest(
                "Arjun",
                "Sharma",
                "9999999999",
                "Updated patient address",
                "8888888888"
        );

        mockMvc.perform(put("/api/v1/patients/{id}", patient.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        AuditLog auditLog = latestAuditLog(AuditAction.PATIENT_UPDATED);
        assertThat(auditLog.getUser().getId()).isEqualTo(patient.getUser().getId());
        assertThat(auditLog.getEntityType()).isEqualTo("Patient");
        assertThat(auditLog.getEntityId()).isEqualTo(patient.getId());
    }

    @Test
    void patientProfileViewCreatesAuditEntry() throws Exception {
        Patient patient = savePatient("patient-view-audit@example.com", "Aarav", "Sharma");

        mockMvc.perform(get("/api/v1/patients/{id}", patient.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser()))))
                .andExpect(status().isOk());

        AuditLog auditLog = latestAuditLog(AuditAction.PATIENT_VIEWED);
        assertThat(auditLog.getUser().getId()).isEqualTo(patient.getUser().getId());
        assertThat(auditLog.getEntityType()).isEqualTo("Patient");
        assertThat(auditLog.getEntityId()).isEqualTo(patient.getId());
    }

    @Test
    void doctorCreationCreatesAuditEntry() throws Exception {
        User admin = saveUser("doctor-create-admin-audit@example.com", Role.ADMIN, true);
        CreateDoctorRequest request = new CreateDoctorRequest(
                "new-doctor-audit@example.com",
                RAW_PASSWORD,
                "Ananya",
                "Rao",
                "Dermatology",
                "AUD102"
        );

        mockMvc.perform(post("/api/v1/admin/doctors")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        AuditLog auditLog = latestAuditLog(AuditAction.DOCTOR_CREATED);
        assertThat(auditLog.getUser().getId()).isEqualTo(admin.getId());
        assertThat(auditLog.getEntityType()).isEqualTo("Doctor");
        assertThat(auditLog.getEntityId()).isNotNull();
    }

    @Test
    void encounterCreationCreatesAuditEntry() throws Exception {
        Patient patient = savePatient("encounter-audit@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("encounter-doctor-audit@example.com", "Ananya", "Rao", "Dermatology", "AUD103", true);
        Appointment appointment = saveAppointment(patient, doctor, AppointmentStatus.SCHEDULED);
        EncounterRequest request = new EncounterRequest(
                appointment.getId(),
                "Skin irritation for three weeks",
                "Mild redness observed"
        );

        mockMvc.perform(post("/api/v1/encounters")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(doctor.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        AuditLog auditLog = latestAuditLog(AuditAction.ENCOUNTER_CREATED);
        assertThat(auditLog.getUser().getId()).isEqualTo(doctor.getUser().getId());
        assertThat(auditLog.getEntityType()).isEqualTo("Encounter");
        assertThat(auditLog.getEntityId()).isNotNull();
    }

    @Test
    void encounterViewCreatesAuditEntry() throws Exception {
        Patient patient = savePatient("encounter-view-audit@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor(
                "encounter-view-doctor-audit@example.com",
                "Ananya",
                "Rao",
                "Dermatology",
                "AUD109",
                true
        );
        Encounter encounter = saveEncounter(saveAppointment(patient, doctor, AppointmentStatus.COMPLETED));

        mockMvc.perform(get("/api/v1/encounters/{id}", encounter.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser()))))
                .andExpect(status().isOk());

        AuditLog auditLog = latestAuditLog(AuditAction.ENCOUNTER_VIEWED);
        assertThat(auditLog.getUser().getId()).isEqualTo(patient.getUser().getId());
        assertThat(auditLog.getEntityType()).isEqualTo("Encounter");
        assertThat(auditLog.getEntityId()).isEqualTo(encounter.getId());
    }

    @Test
    void diagnosisCreationCreatesAuditEntry() throws Exception {
        Patient patient = savePatient("diagnosis-audit@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("diagnosis-doctor-audit@example.com", "Ananya", "Rao", "Dermatology", "AUD104", true);
        Encounter encounter = saveEncounter(saveAppointment(patient, doctor, AppointmentStatus.COMPLETED));
        DiagnosisRequest request = new DiagnosisRequest("L30.9", "Dermatitis", "Unspecified dermatitis");

        mockMvc.perform(post("/api/v1/encounters/{encounterId}/diagnoses", encounter.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(doctor.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        AuditLog auditLog = latestAuditLog(AuditAction.DIAGNOSIS_CREATED);
        assertThat(auditLog.getUser().getId()).isEqualTo(doctor.getUser().getId());
        assertThat(auditLog.getEntityType()).isEqualTo("Diagnosis");
        assertThat(auditLog.getEntityId()).isNotNull();
    }

    @Test
    void prescriptionCreationCreatesAuditEntry() throws Exception {
        Patient patient = savePatient("prescription-audit@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("prescription-doctor-audit@example.com", "Ananya", "Rao", "Dermatology", "AUD105", true);
        Encounter encounter = saveEncounter(saveAppointment(patient, doctor, AppointmentStatus.COMPLETED));
        PrescriptionRequest request = new PrescriptionRequest("Cetirizine", "10 mg", "Once daily", "5 days", "Take after food");

        mockMvc.perform(post("/api/v1/encounters/{encounterId}/prescriptions", encounter.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(doctor.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        AuditLog auditLog = latestAuditLog(AuditAction.PRESCRIPTION_CREATED);
        assertThat(auditLog.getUser().getId()).isEqualTo(doctor.getUser().getId());
        assertThat(auditLog.getEntityType()).isEqualTo("Prescription");
        assertThat(auditLog.getEntityId()).isNotNull();
    }

    @Test
    void prescriptionViewCreatesAuditEntry() throws Exception {
        Patient patient = savePatient("prescription-view-audit@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor(
                "prescription-view-doctor-audit@example.com",
                "Ananya",
                "Rao",
                "Dermatology",
                "AUD110",
                true
        );
        Encounter encounter = saveEncounter(saveAppointment(patient, doctor, AppointmentStatus.COMPLETED));
        savePrescription(encounter);

        mockMvc.perform(get("/api/v1/patients/{patientId}/prescriptions?page=0&size=10", patient.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser()))))
                .andExpect(status().isOk());

        AuditLog auditLog = latestAuditLog(AuditAction.PRESCRIPTION_VIEWED);
        assertThat(auditLog.getUser().getId()).isEqualTo(patient.getUser().getId());
        assertThat(auditLog.getEntityType()).isEqualTo("Patient");
        assertThat(auditLog.getEntityId()).isEqualTo(patient.getId());
    }

    @Test
    void failedBusinessOperationDoesNotCreateMisleadingSuccessAuditEntry() throws Exception {
        Patient patient = savePatient("failed-booking-audit@example.com", "Aarav", "Sharma");
        Doctor inactiveDoctor = saveDoctor(
                "inactive-booking-audit@example.com",
                "Ananya",
                "Rao",
                "Dermatology",
                "AUD106",
                false
        );
        AppointmentRequest request = new AppointmentRequest(
                inactiveDoctor.getId(),
                futureSlot(7, 10, 0),
                "Skin irritation"
        );

        mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertThat(countAuditLogs(AuditAction.APPOINTMENT_CREATED)).isZero();
    }

    @Test
    void adminCanRetrieveAuditLogs() throws Exception {
        User admin = saveUser("audit-admin@example.com", Role.ADMIN, true);
        AuditLog auditLog = saveAuditLog(admin, AuditAction.APPOINTMENT_CREATED, "Appointment", 20L);

        mockMvc.perform(get("/api/v1/admin/audit-logs?action=APPOINTMENT_CREATED&page=0&size=20")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(auditLog.getId()))
                .andExpect(jsonPath("$.content[0].userId").value(admin.getId()))
                .andExpect(jsonPath("$.content[0].userEmail").value(admin.getEmail()))
                .andExpect(jsonPath("$.content[0].action").value("APPOINTMENT_CREATED"))
                .andExpect(jsonPath("$.content[0].entityType").value("Appointment"))
                .andExpect(jsonPath("$.content[0].entityId").value(20L));
    }

    @Test
    void adminAuditLogEndpointSupportsPagination() throws Exception {
        User admin = saveUser("audit-page-admin@example.com", Role.ADMIN, true);
        saveAuditLog(admin, AuditAction.APPOINTMENT_CREATED, "Appointment", 20L);
        saveAuditLog(admin, AuditAction.PATIENT_UPDATED, "Patient", 30L);

        mockMvc.perform(get("/api/v1/admin/audit-logs?page=0&size=1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void adminAuditLogEndpointSupportsUserFilter() throws Exception {
        User admin = saveUser("audit-filter-admin@example.com", Role.ADMIN, true);
        User patientUser = saveUser("audit-filter-patient@example.com", Role.PATIENT, true);
        saveAuditLog(admin, AuditAction.APPOINTMENT_CREATED, "Appointment", 20L);
        saveAuditLog(patientUser, AuditAction.PATIENT_UPDATED, "Patient", 30L);

        mockMvc.perform(get("/api/v1/admin/audit-logs?userId={userId}&page=0&size=20", patientUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(patientUser.getId()))
                .andExpect(jsonPath("$.content[0].action").value("PATIENT_UPDATED"));
    }

    @Test
    void invalidAuditActionFilterReturnsBadRequest() throws Exception {
        User admin = saveUser("audit-invalid-action-admin@example.com", Role.ADMIN, true);

        mockMvc.perform(get("/api/v1/admin/audit-logs?action=NOT_A_REAL_ACTION")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(admin))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid enum value for parameter: action"));
    }

    @Test
    void patientCannotRetrieveAuditLogs() throws Exception {
        Patient patient = savePatient("audit-log-patient@example.com", "Aarav", "Sharma");

        mockMvc.perform(get("/api/v1/admin/audit-logs?page=0&size=20")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void doctorCannotRetrieveAuditLogs() throws Exception {
        Doctor doctor = saveDoctor("audit-log-doctor@example.com", "Ananya", "Rao", "Dermatology", "AUD107", true);

        mockMvc.perform(get("/api/v1/admin/audit-logs?page=0&size=20")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(doctor.getUser()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditResponseDoesNotExposePasswordOrTokenData() throws Exception {
        User admin = saveUser("safe-audit-admin@example.com", Role.ADMIN, true);
        Patient patient = savePatient("safe-audit-patient@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("safe-audit-doctor@example.com", "Ananya", "Rao", "Dermatology", "AUD111", true);
        Encounter encounter = saveEncounter(saveAppointment(patient, doctor, AppointmentStatus.COMPLETED));
        Prescription prescription = savePrescription(encounter);
        saveAuditLog(admin, AuditAction.PRESCRIPTION_CREATED, "Prescription", prescription.getId());

        MvcResult result = mockMvc.perform(get("/api/v1/admin/audit-logs?page=0&size=20")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].password").doesNotExist())
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.content[0].accessToken").doesNotExist())
                .andExpect(jsonPath("$.content[0].jwt").doesNotExist())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody)
                .doesNotContain(RAW_PASSWORD)
                .doesNotContain("Bearer ")
                .doesNotContain("Mild redness observed during examination")
                .doesNotContain("Cetirizine")
                .doesNotContain("10 mg");
    }

    private AuditLog latestAuditLog(AuditAction action) {
        return auditLogRepository
                .findByActionOrderByTimestampDesc(action.name(), PageRequest.of(0, 1))
                .getContent()
                .get(0);
    }

    private long countAuditLogs(AuditAction action) {
        return auditLogRepository
                .findByActionOrderByTimestampDesc(action.name(), PageRequest.of(0, 10))
                .getTotalElements();
    }

    private AuditLog saveAuditLog(User user, AuditAction action, String entityType, Long entityId) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUser(user);
        auditLog.setAction(action.name());
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        return auditLogRepository.saveAndFlush(auditLog);
    }

    private Encounter saveEncounter(Appointment appointment) {
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.saveAndFlush(appointment);

        Encounter encounter = new Encounter();
        encounter.setAppointment(appointment);
        encounter.setPatient(appointment.getPatient());
        encounter.setDoctor(appointment.getDoctor());
        encounter.setVisitDate(LocalDateTime.now());
        encounter.setChiefComplaint("Skin irritation for three weeks");
        encounter.setNotes("Mild redness observed during examination");
        return encounterRepository.saveAndFlush(encounter);
    }

    private Prescription savePrescription(Encounter encounter) {
        Prescription prescription = new Prescription();
        prescription.setEncounter(encounter);
        prescription.setMedicationName("Cetirizine");
        prescription.setDosage("10 mg");
        prescription.setFrequency("Once daily");
        prescription.setDuration("5 days");
        prescription.setInstructions("Take after food");
        return prescriptionRepository.saveAndFlush(prescription);
    }

    private Appointment saveAppointment(Patient patient, Doctor doctor, AppointmentStatus status) {
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDateTime(futureSlot(7, 10, 0));
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
}
