package com.medflow.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medflow.dto.CreateDoctorRequest;
import com.medflow.dto.PatientUpdateRequest;
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
class PatientDoctorControllerIntegrationTest {

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
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private EncounterRepository encounterRepository;

    @Autowired
    private PatientRepository patientRepository;

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
    void patientCanReadOwnProfile() throws Exception {
        Patient patient = savePatient("patient-a@example.com", "Aarav", "Sharma");
        String token = tokenFor(patient.getUser());

        mockMvc.perform(get("/api/v1/patients/{id}", patient.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(patient.getId()))
                .andExpect(jsonPath("$.firstName").value("Aarav"))
                .andExpect(jsonPath("$.lastName").value("Sharma"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void patientCannotReadAnotherPatientsProfile() throws Exception {
        Patient patientA = savePatient("patient-a@example.com", "Aarav", "Sharma");
        Patient patientB = savePatient("patient-b@example.com", "Isha", "Mehta");
        String token = tokenFor(patientA.getUser());

        mockMvc.perform(get("/api/v1/patients/{id}", patientB.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not allowed to access this patient profile"));
    }

    @Test
    void doctorCannotReadPatientProfileWithoutPermission() throws Exception {
        Patient patient = savePatient("doctor-check-patient@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("doctor-check@example.com", "Ananya", "Rao", "Dermatology", "MED050", true);
        String token = tokenFor(doctor.getUser());

        mockMvc.perform(get("/api/v1/patients/{id}", patient.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void patientCanUpdateAllowedOwnProfileFields() throws Exception {
        Patient patient = savePatient("update-own@example.com", "Aarav", "Sharma");
        String token = tokenFor(patient.getUser());
        PatientUpdateRequest request = new PatientUpdateRequest(
                "Arjun",
                "Sharma",
                "9999999999",
                "Updated synthetic address",
                "8888888888"
        );

        mockMvc.perform(put("/api/v1/patients/{id}", patient.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Arjun"))
                .andExpect(jsonPath("$.phone").value("9999999999"))
                .andExpect(jsonPath("$.dateOfBirth").value("2002-05-14"));

        Patient updatedPatient = patientRepository.findById(patient.getId()).orElseThrow();
        assertThat(updatedPatient.getGender()).isEqualTo(Gender.MALE);
        assertThat(updatedPatient.getDateOfBirth()).isEqualTo(LocalDate.of(2002, 5, 14));
    }

    @Test
    void patientCannotUpdateAnotherPatient() throws Exception {
        Patient patientA = savePatient("patient-a@example.com", "Aarav", "Sharma");
        Patient patientB = savePatient("patient-b@example.com", "Isha", "Mehta");
        String token = tokenFor(patientA.getUser());
        PatientUpdateRequest request = new PatientUpdateRequest(
                "Changed",
                "Name",
                "9999999999",
                "Changed address",
                "8888888888"
        );

        mockMvc.perform(put("/api/v1/patients/{id}", patientB.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not allowed to access this patient profile"));

        Patient unchangedPatient = patientRepository.findById(patientB.getId()).orElseThrow();
        assertThat(unchangedPatient.getFirstName()).isEqualTo("Isha");
    }

    @Test
    void patientCannotModifyRoleOrSecurityFieldsThroughProfileUpdate() throws Exception {
        Patient patient = savePatient("secure-update@example.com", "Aarav", "Sharma");
        String originalPassword = patient.getUser().getPassword();
        String token = tokenFor(patient.getUser());

        String payload = """
                {
                  "firstName": "Aarav",
                  "lastName": "Sharma",
                  "phone": "9999999999",
                  "address": "Synthetic address",
                  "emergencyContact": "8888888888",
                  "role": "ADMIN",
                  "enabled": false,
                  "userId": 99,
                  "password": "HackedPassword123!"
                }
                """;

        mockMvc.perform(put("/api/v1/patients/{id}", patient.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body contains invalid or unsupported fields"));

        User unchangedUser = userRepository.findById(patient.getUser().getId()).orElseThrow();
        assertThat(unchangedUser.getRole()).isEqualTo(Role.PATIENT);
        assertThat(unchangedUser.isEnabled()).isTrue();
        assertThat(unchangedUser.getPassword()).isEqualTo(originalPassword);
    }

    @Test
    void authenticatedUserCanRetrieveActiveDoctors() throws Exception {
        Patient patient = savePatient("doctor-list-patient@example.com", "Aarav", "Sharma");
        saveDoctor("active-doctor@example.com", "Ananya", "Rao", "Dermatology", "MED100", true);
        saveDoctor("inactive-doctor@example.com", "Vikram", "Nair", "Cardiology", "MED101", false);
        String token = tokenFor(patient.getUser());

        mockMvc.perform(get("/api/v1/doctors?page=0&size=10")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Ananya"))
                .andExpect(jsonPath("$.content[0].active").value(true));
    }

    @Test
    void doctorListSupportsSpecializationFilter() throws Exception {
        Patient patient = savePatient("doctor-filter-patient@example.com", "Aarav", "Sharma");
        saveDoctor("derma@example.com", "Ananya", "Rao", "Dermatology", "MED200", true);
        saveDoctor("cardio@example.com", "Kabir", "Sen", "Cardiology", "MED201", true);
        String token = tokenFor(patient.getUser());

        mockMvc.perform(get("/api/v1/doctors?specialization=Dermatology&page=0&size=10")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].specialization").value("Dermatology"));
    }

    @Test
    void doctorLookupByIdWorksForActiveDoctor() throws Exception {
        Patient patient = savePatient("doctor-lookup-patient@example.com", "Aarav", "Sharma");
        Doctor doctor = saveDoctor("lookup-doctor@example.com", "Ananya", "Rao", "Dermatology", "MED300", true);
        String token = tokenFor(patient.getUser());

        mockMvc.perform(get("/api/v1/doctors/{id}", doctor.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(doctor.getId()))
                .andExpect(jsonPath("$.firstName").value("Ananya"))
                .andExpect(jsonPath("$.specialization").value("Dermatology"));
    }

    @Test
    void doctorCanRetrieveOwnInactiveProfile() throws Exception {
        Doctor doctor = saveDoctor("inactive-self@example.com", "Ananya", "Rao", "Dermatology", "MED301", false);
        String token = tokenFor(doctor.getUser());

        mockMvc.perform(get("/api/v1/doctors/{id}", doctor.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(doctor.getId()))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void adminCanCreateDoctor() throws Exception {
        User admin = saveUser("admin@example.com", Role.ADMIN, true);
        String token = tokenFor(admin);
        CreateDoctorRequest request = validCreateDoctorRequest("new-doctor@example.com", "MED400");

        mockMvc.perform(post("/api/v1/admin/doctors")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new-doctor@example.com"))
                .andExpect(jsonPath("$.firstName").value("Ananya"))
                .andExpect(jsonPath("$.role").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());

        User doctorUser = userRepository.findByEmail("new-doctor@example.com").orElseThrow();
        assertThat(doctorUser.getRole()).isEqualTo(Role.DOCTOR);
        assertThat(doctorRepository.findByUserId(doctorUser.getId())).isPresent();
    }

    @Test
    void patientCannotCallCreateDoctorAdminEndpoint() throws Exception {
        Patient patient = savePatient("not-admin@example.com", "Aarav", "Sharma");
        String token = tokenFor(patient.getUser());
        CreateDoctorRequest request = validCreateDoctorRequest("blocked-doctor@example.com", "MED500");

        mockMvc.perform(post("/api/v1/admin/doctors")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Forbidden request"));

        assertThat(userRepository.existsByEmail("blocked-doctor@example.com")).isFalse();
    }

    @Test
    void doctorPasswordIsBcryptEncoded() throws Exception {
        User admin = saveUser("admin@example.com", Role.ADMIN, true);
        String token = tokenFor(admin);
        CreateDoctorRequest request = validCreateDoctorRequest("encoded-doctor@example.com", "MED600");

        mockMvc.perform(post("/api/v1/admin/doctors")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        User doctorUser = userRepository.findByEmail("encoded-doctor@example.com").orElseThrow();
        assertThat(doctorUser.getPassword()).isNotEqualTo(RAW_PASSWORD);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, doctorUser.getPassword())).isTrue();
    }

    @Test
    void createdDoctorReceivesDoctorRole() throws Exception {
        User admin = saveUser("admin@example.com", Role.ADMIN, true);
        String token = tokenFor(admin);
        CreateDoctorRequest request = validCreateDoctorRequest("role-doctor@example.com", "MED700");

        mockMvc.perform(post("/api/v1/admin/doctors")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        User doctorUser = userRepository.findByEmail("role-doctor@example.com").orElseThrow();
        assertThat(doctorUser.getRole()).isEqualTo(Role.DOCTOR);
    }

    @Test
    void duplicateDoctorEmailFailsCleanly() throws Exception {
        User admin = saveUser("admin@example.com", Role.ADMIN, true);
        saveUser("duplicate-doctor@example.com", Role.PATIENT, true);
        String token = tokenFor(admin);
        CreateDoctorRequest request = validCreateDoctorRequest("duplicate-doctor@example.com", "MED800");

        mockMvc.perform(post("/api/v1/admin/doctors")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    private CreateDoctorRequest validCreateDoctorRequest(String email, String licenseNumber) {
        return new CreateDoctorRequest(
                email,
                RAW_PASSWORD,
                "Ananya",
                "Rao",
                "Dermatology",
                licenseNumber
        );
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
}
