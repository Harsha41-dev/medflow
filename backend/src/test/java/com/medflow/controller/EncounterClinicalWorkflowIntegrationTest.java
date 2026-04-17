package com.medflow.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medflow.dto.DiagnosisRequest;
import com.medflow.dto.EncounterRequest;
import com.medflow.dto.PrescriptionRequest;
import com.medflow.entity.Appointment;
import com.medflow.entity.AppointmentStatus;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EncounterClinicalWorkflowIntegrationTest {

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
    void assignedDoctorCreatesEncounterSuccessfully() throws Exception {
        Patient patient = savePatient("encounter-patient@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("encounter-doctor@example.com", "Ananya", "Rao", "Dermatology", "MED800", true);
        Appointment appointment = saveAppointment(patient, doctor, AppointmentStatus.SCHEDULED);

        mockMvc.perform(post("/api/v1/encounters")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(doctor.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEncounterRequest(appointment))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.appointmentId").value(appointment.getId()))
                .andExpect(jsonPath("$.patientId").value(patient.getId()))
                .andExpect(jsonPath("$.doctorId").value(doctor.getId()))
                .andExpect(jsonPath("$.chiefComplaint").value("Skin irritation for three weeks"));

        assertThat(encounterRepository.existsByAppointmentId(appointment.getId())).isTrue();
    }

    @Test
    void encounterMarksAppointmentCompleted() throws Exception {
        Patient patient = savePatient("complete-patient@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("complete-doctor@example.com", "Ananya", "Rao", "Dermatology", "MED801", true);
        Appointment appointment = saveAppointment(patient, doctor, AppointmentStatus.SCHEDULED);

        mockMvc.perform(post("/api/v1/encounters")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(doctor.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEncounterRequest(appointment))))
                .andExpect(status().isCreated());

        Appointment completedAppointment = appointmentRepository.findById(appointment.getId()).orElseThrow();
        assertThat(completedAppointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void doctorCannotCreateEncounterForAnotherDoctorsAppointment() throws Exception {
        Patient patient = savePatient("wrong-doctor-patient@example.com", "Aarav", "Sharma");
        Doctor assignedDoctor = saveDoctor("assigned@example.com", "Ananya", "Rao", "Dermatology", "MED802", true);
        Doctor otherDoctor = saveDoctor("other@example.com", "Kabir", "Sen", "Cardiology", "MED803", true);
        Appointment appointment = saveAppointment(patient, assignedDoctor, AppointmentStatus.SCHEDULED);

        mockMvc.perform(post("/api/v1/encounters")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(otherDoctor.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEncounterRequest(appointment))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not allowed to create an encounter for this appointment"));

        Appointment unchangedAppointment = appointmentRepository.findById(appointment.getId()).orElseThrow();
        assertThat(unchangedAppointment.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    void patientCannotCreateEncounter() throws Exception {
        Patient patient = savePatient("patient-create-encounter@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("doctor-create-encounter@example.com", "Ananya", "Rao", "Dermatology", "MED804", true);
        Appointment appointment = saveAppointment(patient, doctor, AppointmentStatus.SCHEDULED);

        mockMvc.perform(post("/api/v1/encounters")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEncounterRequest(appointment))))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelledAppointmentCannotGetEncounter() throws Exception {
        Patient patient = savePatient("cancelled-patient@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("cancelled-doctor@example.com", "Ananya", "Rao", "Dermatology", "MED805", true);
        Appointment appointment = saveAppointment(patient, doctor, AppointmentStatus.CANCELLED);

        mockMvc.perform(post("/api/v1/encounters")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(doctor.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEncounterRequest(appointment))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Encounter can only be created for a scheduled appointment"));
    }

    @Test
    void oneAppointmentCannotHaveTwoEncounters() throws Exception {
        Patient patient = savePatient("duplicate-encounter-patient@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("duplicate-encounter-doctor@example.com", "Ananya", "Rao", "Dermatology", "MED806", true);
        Appointment appointment = saveAppointment(patient, doctor, AppointmentStatus.SCHEDULED);
        String doctorToken = tokenFor(doctor.getUser());

        mockMvc.perform(post("/api/v1/encounters")
                        .header(HttpHeaders.AUTHORIZATION, bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEncounterRequest(appointment))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/encounters")
                        .header(HttpHeaders.AUTHORIZATION, bearer(doctorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEncounterRequest(appointment))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Encounter already exists for this appointment"));
    }

    @Test
    void patientCanViewOwnEncounters() throws Exception {
        Patient patient = savePatient("own-history@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("own-history-doctor@example.com", "Ananya", "Rao", "Dermatology", "MED807", true);
        Encounter encounter = saveEncounter(saveAppointment(patient, doctor, AppointmentStatus.COMPLETED));

        mockMvc.perform(get("/api/v1/patients/{patientId}/encounters?page=0&size=10", patient.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(encounter.getId()));
    }

    @Test
    void patientCannotViewAnotherPatientsEncounters() throws Exception {
        Patient patientA = savePatient("history-a@example.com", "Aarav", "Sharma");
        Patient patientB = savePatient("history-b@example.com", "Isha", "Mehta");
        Doctor doctor = saveDoctor("history-doctor@example.com", "Ananya", "Rao", "Dermatology", "MED808", true);
        saveEncounter(saveAppointment(patientB, doctor, AppointmentStatus.COMPLETED));

        mockMvc.perform(get("/api/v1/patients/{patientId}/encounters?page=0&size=10", patientB.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patientA.getUser()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not allowed to access this patient's clinical records"));
    }

    @Test
    void doctorCanViewPermittedEncounter() throws Exception {
        Patient patient = savePatient("doctor-view-patient@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("doctor-view@example.com", "Ananya", "Rao", "Dermatology", "MED809", true);
        Encounter encounter = saveEncounter(saveAppointment(patient, doctor, AppointmentStatus.COMPLETED));

        mockMvc.perform(get("/api/v1/encounters/{id}", encounter.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(doctor.getUser()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(encounter.getId()))
                .andExpect(jsonPath("$.patientId").value(patient.getId()));
    }

    @Test
    void doctorCannotViewUnrelatedPatientsEncounter() throws Exception {
        Patient patient = savePatient("unrelated-patient@example.com", "Aarav", "Sharma");
        Doctor encounterDoctor = saveDoctor("encounter-owner@example.com", "Ananya", "Rao", "Dermatology", "MED810", true);
        Doctor unrelatedDoctor = saveDoctor("unrelated-doctor@example.com", "Kabir", "Sen", "Cardiology", "MED811", true);
        Encounter encounter = saveEncounter(saveAppointment(patient, encounterDoctor, AppointmentStatus.COMPLETED));

        mockMvc.perform(get("/api/v1/encounters/{id}", encounter.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(unrelatedDoctor.getUser()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not allowed to access this encounter"));
    }

    @Test
    void doctorCannotViewClinicalRecordsThroughOnlyCancelledAppointment() throws Exception {
        Patient patient = savePatient("cancelled-access-patient@example.com", "Aarav", "Sharma");
        Doctor encounterDoctor = saveDoctor("cancelled-access-owner@example.com", "Ananya", "Rao", "Dermatology", "MED821", true);
        Doctor cancelledDoctor = saveDoctor("cancelled-access-doctor@example.com", "Kabir", "Sen", "Cardiology", "MED822", true);
        saveEncounter(saveAppointment(patient, encounterDoctor, AppointmentStatus.COMPLETED));
        saveAppointment(patient, cancelledDoctor, AppointmentStatus.CANCELLED);

        mockMvc.perform(get("/api/v1/patients/{patientId}/encounters?page=0&size=10", patient.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(cancelledDoctor.getUser()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not allowed to access this patient's clinical records"));
    }

    @Test
    void doctorCanViewClinicalRecordsWithScheduledAppointmentRelationship() throws Exception {
        Patient patient = savePatient("scheduled-access-patient@example.com", "Aarav", "Sharma");
        Doctor encounterDoctor = saveDoctor("scheduled-access-owner@example.com", "Ananya", "Rao", "Dermatology", "MED823", true);
        Doctor scheduledDoctor = saveDoctor("scheduled-access-doctor@example.com", "Kabir", "Sen", "Cardiology", "MED824", true);
        Encounter encounter = saveEncounter(saveAppointment(patient, encounterDoctor, AppointmentStatus.COMPLETED));
        saveAppointment(patient, scheduledDoctor, AppointmentStatus.SCHEDULED);

        mockMvc.perform(get("/api/v1/patients/{patientId}/encounters?page=0&size=10", patient.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(scheduledDoctor.getUser()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(encounter.getId()));
    }

    @Test
    void assignedDoctorCanAddDiagnosis() throws Exception {
        Patient patient = savePatient("diagnosis-patient@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("diagnosis-doctor@example.com", "Ananya", "Rao", "Dermatology", "MED812", true);
        Encounter encounter = saveEncounter(saveAppointment(patient, doctor, AppointmentStatus.COMPLETED));

        mockMvc.perform(post("/api/v1/encounters/{encounterId}/diagnoses", encounter.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(doctor.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDiagnosisRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.encounterId").value(encounter.getId()))
                .andExpect(jsonPath("$.diagnosisCode").value("L30.9"));
    }

    @Test
    void differentDoctorCannotAddDiagnosis() throws Exception {
        Patient patient = savePatient("wrong-diagnosis-patient@example.com", "Aarav", "Sharma");
        Doctor ownerDoctor = saveDoctor("diagnosis-owner@example.com", "Ananya", "Rao", "Dermatology", "MED813", true);
        Doctor otherDoctor = saveDoctor("diagnosis-other@example.com", "Kabir", "Sen", "Cardiology", "MED814", true);
        Encounter encounter = saveEncounter(saveAppointment(patient, ownerDoctor, AppointmentStatus.COMPLETED));

        mockMvc.perform(post("/api/v1/encounters/{encounterId}/diagnoses", encounter.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(otherDoctor.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDiagnosisRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not allowed to modify this encounter"));
    }

    @Test
    void assignedDoctorCanCreatePrescription() throws Exception {
        Patient patient = savePatient("prescription-patient@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("prescription-doctor@example.com", "Ananya", "Rao", "Dermatology", "MED815", true);
        Encounter encounter = saveEncounter(saveAppointment(patient, doctor, AppointmentStatus.COMPLETED));

        mockMvc.perform(post("/api/v1/encounters/{encounterId}/prescriptions", encounter.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(doctor.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPrescriptionRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.encounterId").value(encounter.getId()))
                .andExpect(jsonPath("$.medicationName").value("Cetirizine"));
    }

    @Test
    void differentDoctorCannotCreatePrescription() throws Exception {
        Patient patient = savePatient("wrong-prescription-patient@example.com", "Aarav", "Sharma");
        Doctor ownerDoctor = saveDoctor("prescription-owner@example.com", "Ananya", "Rao", "Dermatology", "MED816", true);
        Doctor otherDoctor = saveDoctor("prescription-other@example.com", "Kabir", "Sen", "Cardiology", "MED817", true);
        Encounter encounter = saveEncounter(saveAppointment(patient, ownerDoctor, AppointmentStatus.COMPLETED));

        mockMvc.perform(post("/api/v1/encounters/{encounterId}/prescriptions", encounter.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(otherDoctor.getUser())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPrescriptionRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not allowed to modify this encounter"));
    }

    @Test
    void patientCanViewOwnPrescriptions() throws Exception {
        Patient patient = savePatient("own-prescription@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("own-prescription-doctor@example.com", "Ananya", "Rao", "Dermatology", "MED818", true);
        Encounter encounter = saveEncounter(saveAppointment(patient, doctor, AppointmentStatus.COMPLETED));
        Prescription prescription = savePrescription(encounter);

        mockMvc.perform(get("/api/v1/patients/{patientId}/prescriptions?page=0&size=10", patient.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patient.getUser()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(prescription.getId()))
                .andExpect(jsonPath("$.content[0].medicationName").value("Cetirizine"));
    }

    @Test
    void patientCannotViewAnotherPatientsPrescriptions() throws Exception {
        Patient patientA = savePatient("prescription-a@example.com", "Aarav", "Sharma");
        Patient patientB = savePatient("prescription-b@example.com", "Isha", "Mehta");
        Doctor doctor = saveDoctor("prescription-history-doctor@example.com", "Ananya", "Rao", "Dermatology", "MED819", true);
        Encounter encounter = saveEncounter(saveAppointment(patientB, doctor, AppointmentStatus.COMPLETED));
        savePrescription(encounter);

        mockMvc.perform(get("/api/v1/patients/{patientId}/prescriptions?page=0&size=10", patientB.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(patientA.getUser()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not allowed to access this patient's clinical records"));
    }

    @Test
    void doctorCanViewPrescriptionsForPatientWithAppointmentRelationship() throws Exception {
        Patient patient = savePatient("doctor-prescription-patient@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("doctor-prescription-view@example.com", "Ananya", "Rao", "Dermatology", "MED820", true);
        Encounter encounter = saveEncounter(saveAppointment(patient, doctor, AppointmentStatus.COMPLETED));
        Prescription prescription = savePrescription(encounter);

        mockMvc.perform(get("/api/v1/patients/{patientId}/prescriptions?page=0&size=10", patient.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(doctor.getUser()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(prescription.getId()));
    }

    private EncounterRequest validEncounterRequest(Appointment appointment) {
        return new EncounterRequest(
                appointment.getId(),
                "Skin irritation for three weeks",
                "Mild redness observed during examination"
        );
    }

    private DiagnosisRequest validDiagnosisRequest() {
        return new DiagnosisRequest(
                "L30.9",
                "Dermatitis",
                "Unspecified dermatitis"
        );
    }

    private PrescriptionRequest validPrescriptionRequest() {
        return new PrescriptionRequest(
                "Cetirizine",
                "10 mg",
                "Once daily",
                "5 days",
                "Take after food"
        );
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
        appointment.setAppointmentDateTime(futureSlot());
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

    private LocalDateTime futureSlot() {
        return LocalDateTime.now()
                .plusDays(7)
                .withHour(10)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }
}
