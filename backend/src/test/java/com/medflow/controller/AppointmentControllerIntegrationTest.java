package com.medflow.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medflow.dto.AppointmentRequest;
import com.medflow.dto.AppointmentSlotResponse;
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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AppointmentControllerIntegrationTest {

    private static final String RAW_PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private EncounterRepository encounterRepository;

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
    void patientSeesAvailableSlotsForSelectedDoctorAndDate() throws Exception {
        Patient patient = savePatient("slots-patient@example.com", "Aarav", "Sharma");
        Patient otherPatient = savePatient("slots-other-patient@example.com", "Isha", "Mehta");
        Doctor doctor = saveDoctor("slots-doctor@example.com", "Ananya", "Rao", "Dermatology", "MED120", true);
        LocalDate date = LocalDate.now().plusDays(7);
        LocalDateTime scheduledTime = date.atTime(10, 0);
        LocalDateTime cancelledTime = date.atTime(10, 30);
        saveAppointment(otherPatient, doctor, scheduledTime, AppointmentStatus.SCHEDULED);
        saveAppointment(otherPatient, doctor, cancelledTime, AppointmentStatus.CANCELLED);

        String json = mockMvc.perform(get("/api/v1/appointments/available-slots")
                        .param("doctorId", doctor.getId().toString())
                        .param("date", date.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser()))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<AppointmentSlotResponse> slots = objectMapper.readValue(json, new TypeReference<>() {
        });

        assertThat(slots).hasSize(15);
        assertThat(slots)
                .extracting(AppointmentSlotResponse::appointmentDateTime)
                .doesNotContain(scheduledTime)
                .contains(cancelledTime);
    }

    @Test
    void doctorCannotViewPatientAvailableSlots() throws Exception {
        Doctor doctor = saveDoctor("slots-forbidden-doctor@example.com", "Ananya", "Rao", "Dermatology", "MED121", true);
        LocalDate date = LocalDate.now().plusDays(7);

        mockMvc.perform(get("/api/v1/appointments/available-slots")
                        .param("doctorId", doctor.getId().toString())
                        .param("date", date.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(doctor.getUser()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void patientBooksAppointmentSuccessfully() throws Exception {
        Patient patient = savePatient("booker@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("doctor@example.com", "Ananya", "Rao", "Dermatology", "MED100", true);
        LocalDateTime appointmentTime = futureSlot(7, 10, 0);
        AppointmentRequest request = new AppointmentRequest(doctor.getId(), appointmentTime, "Skin irritation");

        mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(patient.getId()))
                .andExpect(jsonPath("$.doctorId").value(doctor.getId()))
                .andExpect(jsonPath("$.appointmentDateTime").exists())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.reason").value("Skin irritation"));
    }

    @Test
    void patientCannotSubmitArbitraryPatientIdWhenBooking() throws Exception {
        Patient patient = savePatient("safe-booker@example.com", "Aarav", "Sharma");
        Patient otherPatient = savePatient("other-patient@example.com", "Isha", "Mehta");
        Doctor doctor = saveDoctor("doctor@example.com", "Ananya", "Rao", "Dermatology", "MED101", true);
        String payload = """
                {
                  "patientId": %d,
                  "doctorId": %d,
                  "appointmentDateTime": "%s",
                  "reason": "Skin irritation"
                }
                """.formatted(otherPatient.getId(), doctor.getId(), futureSlot(7, 10, 0));

        mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body contains invalid or unsupported fields"));

        assertThat(appointmentRepository.count()).isZero();
    }

    @Test
    void patientCannotBookInactiveDoctor() throws Exception {
        Patient patient = savePatient("inactive-booker@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("inactive-doctor@example.com", "Ananya", "Rao", "Dermatology", "MED102", false);
        AppointmentRequest request = new AppointmentRequest(doctor.getId(), futureSlot(7, 10, 0), "Skin irritation");

        mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Doctor is not active"));
    }

    @Test
    void appointmentInPastIsRejected() throws Exception {
        Patient patient = savePatient("past-booker@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("past-doctor@example.com", "Ananya", "Rao", "Dermatology", "MED103", true);
        AppointmentRequest request = new AppointmentRequest(doctor.getId(), pastSlot(), "Skin irritation");

        mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidThirtyMinuteSlotIsRejected() throws Exception {
        Patient patient = savePatient("slot-booker@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("slot-doctor@example.com", "Ananya", "Rao", "Dermatology", "MED104", true);
        AppointmentRequest request = new AppointmentRequest(doctor.getId(), futureSlot(7, 9, 10), "Skin irritation");

        mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Appointments must use 30-minute slots"));
    }

    @Test
    void sameDoctorAndTimeCannotBeDoubleBooked() throws Exception {
        Patient patientA = savePatient("patient-a@example.com", "Aarav", "Sharma");
        Patient patientB = savePatient("patient-b@example.com", "Isha", "Mehta");
        Doctor doctor = saveDoctor("busy-doctor@example.com", "Ananya", "Rao", "Dermatology", "MED105", true);
        LocalDateTime appointmentTime = futureSlot(7, 10, 0);

        book(patientA, doctor, appointmentTime);

        AppointmentRequest request = new AppointmentRequest(doctor.getId(), appointmentTime, "Follow up");
        mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patientB.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Doctor already has a scheduled appointment at this time"));
    }

    @Test
    void differentDoctorsCanHaveSameAppointmentTime() throws Exception {
        Patient patient = savePatient("same-time-patient@example.com", "Aarav", "Sharma");
        Doctor doctorA = saveDoctor("doctor-a@example.com", "Ananya", "Rao", "Dermatology", "MED106", true);
        Doctor doctorB = saveDoctor("doctor-b@example.com", "Kabir", "Sen", "Cardiology", "MED107", true);
        LocalDateTime appointmentTime = futureSlot(7, 10, 0);

        book(patient, doctorA, appointmentTime);

        AppointmentRequest request = new AppointmentRequest(doctorB.getId(), appointmentTime, "Heart checkup");
        mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.doctorId").value(doctorB.getId()));

        assertThat(appointmentRepository.count()).isEqualTo(2);
    }

    @Test
    void patientSeesOnlyOwnAppointments() throws Exception {
        Patient patientA = savePatient("own-list-a@example.com", "Aarav", "Sharma");
        Patient patientB = savePatient("own-list-b@example.com", "Isha", "Mehta");
        Doctor doctor = saveDoctor("list-doctor@example.com", "Ananya", "Rao", "Dermatology", "MED108", true);
        saveAppointment(patientA, doctor, futureSlot(7, 10, 0), AppointmentStatus.SCHEDULED);
        saveAppointment(patientB, doctor, futureSlot(7, 10, 30), AppointmentStatus.SCHEDULED);

        mockMvc.perform(get("/api/v1/appointments?page=0&size=10")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patientA.getUser()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].patientId").value(patientA.getId()));
    }

    @Test
    void appointmentListSupportsStatusFilter() throws Exception {
        Patient patient = savePatient("status-list@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("status-doctor@example.com", "Ananya", "Rao", "Dermatology", "MED109", true);
        saveAppointment(patient, doctor, futureSlot(7, 10, 0), AppointmentStatus.SCHEDULED);
        saveAppointment(patient, doctor, futureSlot(7, 10, 30), AppointmentStatus.CANCELLED);

        mockMvc.perform(get("/api/v1/appointments?status=CANCELLED&page=0&size=10")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("CANCELLED"));
    }

    @Test
    void adminCanSeeAllAppointments() throws Exception {
        User admin = saveUser("admin@example.com", Role.ADMIN, true);
        Patient patientA = savePatient("admin-list-a@example.com", "Aarav", "Sharma");
        Patient patientB = savePatient("admin-list-b@example.com", "Isha", "Mehta");
        Doctor doctor = saveDoctor("admin-list-doctor@example.com", "Ananya", "Rao", "Dermatology", "MED119", true);
        saveAppointment(patientA, doctor, futureSlot(7, 10, 0), AppointmentStatus.SCHEDULED);
        saveAppointment(patientB, doctor, futureSlot(7, 10, 30), AppointmentStatus.SCHEDULED);

        mockMvc.perform(get("/api/v1/appointments?page=0&size=10")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void patientCannotRetrieveAnotherPatientsAppointment() throws Exception {
        Patient patientA = savePatient("retrieve-a@example.com", "Aarav", "Sharma");
        Patient patientB = savePatient("retrieve-b@example.com", "Isha", "Mehta");
        Doctor doctor = saveDoctor("retrieve-doctor@example.com", "Ananya", "Rao", "Dermatology", "MED110", true);
        Appointment appointment = saveAppointment(patientB, doctor, futureSlot(7, 10, 0), AppointmentStatus.SCHEDULED);

        mockMvc.perform(get("/api/v1/appointments/{id}", appointment.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patientA.getUser()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not allowed to access this appointment"));
    }

    @Test
    void doctorSeesOnlyAssignedAppointments() throws Exception {
        Patient patient = savePatient("doctor-list-patient@example.com", "Aarav", "Sharma");
        Doctor doctorA = saveDoctor("assigned-doctor@example.com", "Ananya", "Rao", "Dermatology", "MED111", true);
        Doctor doctorB = saveDoctor("other-doctor@example.com", "Kabir", "Sen", "Cardiology", "MED112", true);
        saveAppointment(patient, doctorA, futureSlot(7, 10, 0), AppointmentStatus.SCHEDULED);
        saveAppointment(patient, doctorB, futureSlot(7, 10, 30), AppointmentStatus.SCHEDULED);

        mockMvc.perform(get("/api/v1/appointments?page=0&size=10")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(doctorA.getUser()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].doctorId").value(doctorA.getId()));
    }

    @Test
    void doctorCannotRetrieveAnotherDoctorsAppointment() throws Exception {
        Patient patient = savePatient("doctor-retrieve-patient@example.com", "Aarav", "Sharma");
        Doctor doctorA = saveDoctor("doctor-a@example.com", "Ananya", "Rao", "Dermatology", "MED113", true);
        Doctor doctorB = saveDoctor("doctor-b@example.com", "Kabir", "Sen", "Cardiology", "MED114", true);
        Appointment appointment = saveAppointment(patient, doctorB, futureSlot(7, 10, 0), AppointmentStatus.SCHEDULED);

        mockMvc.perform(get("/api/v1/appointments/{id}", appointment.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(doctorA.getUser()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not allowed to access this appointment"));
    }

    @Test
    void patientCanCancelOwnScheduledAppointment() throws Exception {
        Patient patient = savePatient("cancel-own@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("cancel-doctor@example.com", "Ananya", "Rao", "Dermatology", "MED115", true);
        Appointment appointment = saveAppointment(patient, doctor, futureSlot(7, 10, 0), AppointmentStatus.SCHEDULED);

        mockMvc.perform(put("/api/v1/appointments/{id}/cancel", appointment.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        Appointment cancelled = appointmentRepository.findById(appointment.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void patientCannotCancelAnotherPatientsAppointment() throws Exception {
        Patient patientA = savePatient("cancel-a@example.com", "Aarav", "Sharma");
        Patient patientB = savePatient("cancel-b@example.com", "Isha", "Mehta");
        Doctor doctor = saveDoctor("cancel-other-doctor@example.com", "Ananya", "Rao", "Dermatology", "MED116", true);
        Appointment appointment = saveAppointment(patientB, doctor, futureSlot(7, 10, 0), AppointmentStatus.SCHEDULED);

        mockMvc.perform(put("/api/v1/appointments/{id}/cancel", appointment.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patientA.getUser()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not allowed to cancel this appointment"));

        Appointment unchanged = appointmentRepository.findById(appointment.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    void cancelledSlotCanBeBookedAgain() throws Exception {
        Patient patientA = savePatient("rebook-a@example.com", "Aarav", "Sharma");
        Patient patientB = savePatient("rebook-b@example.com", "Isha", "Mehta");
        Doctor doctor = saveDoctor("rebook-doctor@example.com", "Ananya", "Rao", "Dermatology", "MED117", true);
        LocalDateTime appointmentTime = futureSlot(7, 10, 0);
        Appointment appointment = saveAppointment(patientA, doctor, appointmentTime, AppointmentStatus.SCHEDULED);

        mockMvc.perform(put("/api/v1/appointments/{id}/cancel", appointment.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patientA.getUser()))))
                .andExpect(status().isOk());

        AppointmentRequest request = new AppointmentRequest(doctor.getId(), appointmentTime, "Rebooked slot");
        mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patientB.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(patientB.getId()))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        Appointment cancelledAppointment = appointmentRepository.findById(appointment.getId()).orElseThrow();
        assertThat(cancelledAppointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(appointmentRepository.count()).isEqualTo(2);
    }

    @Test
    void cancellingAlreadyCancelledAppointmentFailsCleanly() throws Exception {
        Patient patient = savePatient("cancel-twice@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("cancel-twice-doctor@example.com", "Ananya", "Rao", "Dermatology", "MED118", true);
        Appointment appointment = saveAppointment(patient, doctor, futureSlot(7, 10, 0), AppointmentStatus.CANCELLED);

        mockMvc.perform(put("/api/v1/appointments/{id}/cancel", appointment.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only scheduled appointments can be cancelled"));
    }

    private Appointment book(Patient patient, Doctor doctor, LocalDateTime appointmentTime) throws Exception {
        AppointmentRequest request = new AppointmentRequest(doctor.getId(), appointmentTime, "Skin irritation");

        mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        return appointmentRepository
                .findByDoctorIdAndStatusOrderByAppointmentDateTimeDesc(
                        doctor.getId(),
                        AppointmentStatus.SCHEDULED,
                        org.springframework.data.domain.PageRequest.of(0, 1)
                )
                .getContent()
                .get(0);
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
}
