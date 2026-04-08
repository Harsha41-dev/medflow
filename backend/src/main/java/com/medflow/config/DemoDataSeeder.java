package com.medflow.config;

import com.medflow.entity.Appointment;
import com.medflow.entity.AppointmentStatus;
import com.medflow.entity.AuditAction;
import com.medflow.entity.AuditLog;
import com.medflow.entity.Diagnosis;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.demo.seed-enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final String DEMO_PASSWORD = "Password123!";
    private static final String ENRICHED_DEMO_MARKER_EMAIL = "patient.tara@medflow.demo";

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final EncounterRepository encounterRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(ENRICHED_DEMO_MARKER_EMAIL)) {
            log.info("Enriched demo data already exists; skipping seed.");
            return;
        }

        User adminUser = createOrUpdateUser("admin@medflow.demo", Role.ADMIN, true);

        Doctor doctorAsha = createOrUpdateDoctor(
                "doctor.asha@medflow.demo",
                "Asha",
                "Mehta",
                "Cardiology",
                "MED-DEMO-1001",
                true
        );
        Doctor doctorRohan = createOrUpdateDoctor(
                "doctor.rohan@medflow.demo",
                "Rohan",
                "Kapoor",
                "General Medicine",
                "MED-DEMO-1002",
                true
        );
        Doctor doctorSara = createOrUpdateDoctor(
                "doctor.sara@medflow.demo",
                "Sara",
                "Dsouza",
                "Dermatology",
                "MED-DEMO-1004",
                true
        );
        Doctor doctorVikram = createOrUpdateDoctor(
                "doctor.vikram@medflow.demo",
                "Vikram",
                "Sethi",
                "Orthopedics",
                "MED-DEMO-1005",
                true
        );
        Doctor doctorLeela = createOrUpdateDoctor(
                "doctor.leela@medflow.demo",
                "Leela",
                "Menon",
                "Pediatrics",
                "MED-DEMO-1006",
                true
        );
        Doctor doctorNeha = createOrUpdateDoctor(
                "doctor.neha@medflow.demo",
                "Neha",
                "Iyer",
                "Dermatology",
                "MED-DEMO-1003",
                false
        );

        Patient patientPriya = createOrUpdatePatient(
                "patient.priya@medflow.demo",
                "Priya",
                "Nair",
                LocalDate.of(1998, 3, 14),
                Gender.FEMALE,
                "9876543210",
                "12 Demo Street, Bengaluru",
                "Amit Nair - 9876500001"
        );
        Patient patientArjun = createOrUpdatePatient(
                "patient.arjun@medflow.demo",
                "Arjun",
                "Kumar",
                LocalDate.of(1995, 8, 22),
                Gender.MALE,
                "9876543211",
                "45 Sample Avenue, Pune",
                "Nisha Kumar - 9876500002"
        );
        Patient patientMeera = createOrUpdatePatient(
                "patient.meera@medflow.demo",
                "Meera",
                "Shah",
                LocalDate.of(2002, 11, 5),
                Gender.FEMALE,
                "9876543212",
                "78 Training Layout, Chennai",
                "Ravi Shah - 9876500003"
        );
        Patient patientOmar = createOrUpdatePatient(
                "patient.omar@medflow.demo",
                "Omar",
                "Khan",
                LocalDate.of(1989, 6, 9),
                Gender.MALE,
                "9876543213",
                "21 Practice Road, Hyderabad",
                "Sara Khan - 9876500004"
        );
        Patient patientKavya = createOrUpdatePatient(
                "patient.kavya@medflow.demo",
                "Kavya",
                "Rao",
                LocalDate.of(1992, 1, 19),
                Gender.FEMALE,
                "9876543214",
                "33 Learning Colony, Mumbai",
                "Dev Rao - 9876500005"
        );
        Patient patientTara = createOrUpdatePatient(
                "patient.tara@medflow.demo",
                "Tara",
                "Joshi",
                LocalDate.of(2016, 9, 3),
                Gender.FEMALE,
                "9876543215",
                "66 Clinic Demo Nagar, Kochi",
                "Anil Joshi - 9876500006"
        );

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        Appointment priyaHeartFollowUp = createAppointment(
                patientPriya,
                doctorAsha,
                tomorrow.atTime(10, 0),
                AppointmentStatus.SCHEDULED,
                "Blood pressure and chest discomfort follow-up"
        );
        createAppointment(
                patientArjun,
                doctorRohan,
                tomorrow.atTime(11, 30),
                AppointmentStatus.SCHEDULED,
                "Fever, cough, and body ache"
        );
        createAppointment(
                patientMeera,
                doctorSara,
                tomorrow.atTime(12, 0),
                AppointmentStatus.SCHEDULED,
                "Recurring skin irritation review"
        );
        createAppointment(
                patientOmar,
                doctorVikram,
                tomorrow.plusDays(1).atTime(9, 30),
                AppointmentStatus.SCHEDULED,
                "Knee pain after morning run"
        );
        createAppointment(
                patientTara,
                doctorLeela,
                tomorrow.plusDays(1).atTime(10, 30),
                AppointmentStatus.SCHEDULED,
                "Child fever and sore throat"
        );
        createAppointment(
                patientKavya,
                doctorRohan,
                tomorrow.plusDays(2).atTime(15, 0),
                AppointmentStatus.SCHEDULED,
                "Migraine and light sensitivity"
        );
        Appointment priyaCancelled = createAppointment(
                patientPriya,
                doctorRohan,
                tomorrow.plusDays(2).atTime(14, 30),
                AppointmentStatus.CANCELLED,
                "Headache review cancelled by patient"
        );
        Appointment arjunCancelled = createAppointment(
                patientArjun,
                doctorSara,
                tomorrow.plusDays(3).atTime(9, 30),
                AppointmentStatus.CANCELLED,
                "Acne follow-up cancelled"
        );
        createAppointment(
                patientMeera,
                doctorRohan,
                LocalDateTime.now().minusDays(4).withHour(10).withMinute(30).withSecond(0).withNano(0),
                AppointmentStatus.NO_SHOW,
                "Missed wellness consultation"
        );

        Appointment priyaCompleted = createAppointment(
                patientPriya,
                doctorAsha,
                LocalDateTime.now().minusDays(9).withHour(10).withMinute(0).withSecond(0).withNano(0),
                AppointmentStatus.COMPLETED,
                "Blood pressure review"
        );
        Encounter priyaEncounter = createEncounter(
                priyaCompleted,
                "Elevated blood pressure readings",
                "Reviewed home BP log and lifestyle factors. Advised routine monitoring and follow-up."
        );
        Diagnosis priyaDiagnosis = createDiagnosis(
                priyaEncounter,
                "I10",
                "Essential hypertension",
                "Demo diagnosis for elevated blood pressure follow-up."
        );
        Prescription priyaPrescription = createPrescription(
                priyaEncounter,
                "Amlodipine",
                "5 mg",
                "Once daily",
                "30 days",
                "Take after breakfast and record daily blood pressure readings."
        );

        Appointment arjunCompleted = createAppointment(
                patientArjun,
                doctorRohan,
                LocalDateTime.now().minusDays(6).withHour(15).withMinute(0).withSecond(0).withNano(0),
                AppointmentStatus.COMPLETED,
                "Cough and fatigue review"
        );
        Encounter arjunEncounter = createEncounter(
                arjunCompleted,
                "Cough, fever, and fatigue",
                "Vitals reviewed. Suggested rest, hydration, and return if breathing symptoms worsen."
        );
        Diagnosis arjunDiagnosis = createDiagnosis(
                arjunEncounter,
                "J06.9",
                "Acute upper respiratory infection",
                "Demo respiratory diagnosis for a completed general medicine encounter."
        );
        Prescription arjunPrescription = createPrescription(
                arjunEncounter,
                "Paracetamol",
                "500 mg",
                "Twice daily",
                "3 days",
                "Use only if fever or body ache is present."
        );

        Appointment meeraCompleted = createAppointment(
                patientMeera,
                doctorSara,
                LocalDateTime.now().minusDays(12).withHour(11).withMinute(0).withSecond(0).withNano(0),
                AppointmentStatus.COMPLETED,
                "Dry itchy skin patches"
        );
        Encounter meeraEncounter = createEncounter(
                meeraCompleted,
                "Skin rash with itching",
                "Examined localized rash. Advised moisturizer use and avoiding suspected triggers."
        );
        Diagnosis meeraDiagnosis = createDiagnosis(
                meeraEncounter,
                "L30.9",
                "Dermatitis",
                "Demo dermatology diagnosis for itchy skin rash."
        );
        Prescription meeraPrescription = createPrescription(
                meeraEncounter,
                "Cetirizine",
                "10 mg",
                "Once at night",
                "5 days",
                "Avoid driving if drowsiness occurs."
        );
        Prescription meeraTopicalPrescription = createPrescription(
                meeraEncounter,
                "Calamine lotion",
                "Thin layer",
                "Twice daily",
                "7 days",
                "Apply gently on affected area."
        );

        Appointment omarCompleted = createAppointment(
                patientOmar,
                doctorVikram,
                LocalDateTime.now().minusDays(15).withHour(16).withMinute(0).withSecond(0).withNano(0),
                AppointmentStatus.COMPLETED,
                "Knee pain after exercise"
        );
        Encounter omarEncounter = createEncounter(
                omarCompleted,
                "Knee strain after running",
                "No major swelling noted in this demo note. Recommended rest and gradual return to activity."
        );
        Diagnosis omarDiagnosis = createDiagnosis(
                omarEncounter,
                "S86.9",
                "Soft tissue strain",
                "Demo orthopedics diagnosis for exercise-related knee pain."
        );
        Prescription omarPrescription = createPrescription(
                omarEncounter,
                "Ibuprofen",
                "400 mg",
                "After food when needed",
                "3 days",
                "Avoid if stomach pain or allergy history is present."
        );

        Appointment kavyaCompleted = createAppointment(
                patientKavya,
                doctorRohan,
                LocalDateTime.now().minusDays(18).withHour(14).withMinute(0).withSecond(0).withNano(0),
                AppointmentStatus.COMPLETED,
                "Migraine follow-up"
        );
        Encounter kavyaEncounter = createEncounter(
                kavyaCompleted,
                "Headache with light sensitivity",
                "Discussed triggers, sleep pattern, hydration, and warning symptoms that need urgent review."
        );
        Diagnosis kavyaDiagnosis = createDiagnosis(
                kavyaEncounter,
                "G43.9",
                "Migraine",
                "Demo diagnosis for headache pattern follow-up."
        );
        Prescription kavyaPrescription = createPrescription(
                kavyaEncounter,
                "Sumatriptan",
                "50 mg",
                "As advised during attack",
                "As needed",
                "Use only according to clinician guidance in this demo workflow."
        );

        Appointment taraCompleted = createAppointment(
                patientTara,
                doctorLeela,
                LocalDateTime.now().minusDays(20).withHour(9).withMinute(30).withSecond(0).withNano(0),
                AppointmentStatus.COMPLETED,
                "Pediatric fever follow-up"
        );
        Encounter taraEncounter = createEncounter(
                taraCompleted,
                "Low-grade fever and sore throat",
                "Parent reported improving symptoms. Advised fluids, observation, and follow-up if fever persists."
        );
        Diagnosis taraDiagnosis = createDiagnosis(
                taraEncounter,
                "R50.9",
                "Fever, unspecified",
                "Demo pediatric diagnosis for fever follow-up."
        );
        Prescription taraPrescription = createPrescription(
                taraEncounter,
                "Oral rehydration solution",
                "Small frequent sips",
                "Every few hours",
                "2 days",
                "Continue regular meals as tolerated."
        );

        createAudit(adminUser, AuditAction.DOCTOR_CREATED, "Doctor", doctorAsha.getId());
        createAudit(adminUser, AuditAction.DOCTOR_CREATED, "Doctor", doctorRohan.getId());
        createAudit(adminUser, AuditAction.DOCTOR_CREATED, "Doctor", doctorSara.getId());
        createAudit(adminUser, AuditAction.DOCTOR_CREATED, "Doctor", doctorVikram.getId());
        createAudit(adminUser, AuditAction.DOCTOR_CREATED, "Doctor", doctorLeela.getId());
        createAudit(adminUser, AuditAction.DOCTOR_CREATED, "Doctor", doctorNeha.getId());
        createAudit(patientPriya.getUser(), AuditAction.PATIENT_UPDATED, "Patient", patientPriya.getId());
        createAudit(patientPriya.getUser(), AuditAction.APPOINTMENT_CREATED, "Appointment", priyaHeartFollowUp.getId());
        createAudit(patientPriya.getUser(), AuditAction.APPOINTMENT_CANCELLED, "Appointment", priyaCancelled.getId());
        createAudit(patientArjun.getUser(), AuditAction.APPOINTMENT_CANCELLED, "Appointment", arjunCancelled.getId());
        createAudit(doctorAsha.getUser(), AuditAction.ENCOUNTER_CREATED, "Encounter", priyaEncounter.getId());
        createAudit(doctorAsha.getUser(), AuditAction.DIAGNOSIS_CREATED, "Diagnosis", priyaDiagnosis.getId());
        createAudit(doctorAsha.getUser(), AuditAction.PRESCRIPTION_CREATED, "Prescription", priyaPrescription.getId());
        createAudit(doctorRohan.getUser(), AuditAction.ENCOUNTER_CREATED, "Encounter", arjunEncounter.getId());
        createAudit(doctorRohan.getUser(), AuditAction.DIAGNOSIS_CREATED, "Diagnosis", arjunDiagnosis.getId());
        createAudit(doctorRohan.getUser(), AuditAction.PRESCRIPTION_CREATED, "Prescription", arjunPrescription.getId());
        createAudit(doctorSara.getUser(), AuditAction.ENCOUNTER_CREATED, "Encounter", meeraEncounter.getId());
        createAudit(doctorSara.getUser(), AuditAction.DIAGNOSIS_CREATED, "Diagnosis", meeraDiagnosis.getId());
        createAudit(doctorSara.getUser(), AuditAction.PRESCRIPTION_CREATED, "Prescription", meeraPrescription.getId());
        createAudit(doctorSara.getUser(), AuditAction.PRESCRIPTION_CREATED, "Prescription", meeraTopicalPrescription.getId());
        createAudit(doctorVikram.getUser(), AuditAction.ENCOUNTER_CREATED, "Encounter", omarEncounter.getId());
        createAudit(doctorVikram.getUser(), AuditAction.DIAGNOSIS_CREATED, "Diagnosis", omarDiagnosis.getId());
        createAudit(doctorVikram.getUser(), AuditAction.PRESCRIPTION_CREATED, "Prescription", omarPrescription.getId());
        createAudit(doctorRohan.getUser(), AuditAction.ENCOUNTER_CREATED, "Encounter", kavyaEncounter.getId());
        createAudit(doctorRohan.getUser(), AuditAction.DIAGNOSIS_CREATED, "Diagnosis", kavyaDiagnosis.getId());
        createAudit(doctorRohan.getUser(), AuditAction.PRESCRIPTION_CREATED, "Prescription", kavyaPrescription.getId());
        createAudit(doctorLeela.getUser(), AuditAction.ENCOUNTER_CREATED, "Encounter", taraEncounter.getId());
        createAudit(doctorLeela.getUser(), AuditAction.DIAGNOSIS_CREATED, "Diagnosis", taraDiagnosis.getId());
        createAudit(doctorLeela.getUser(), AuditAction.PRESCRIPTION_CREATED, "Prescription", taraPrescription.getId());
        createAudit(doctorRohan.getUser(), AuditAction.SYMPTOM_PATTERN_PREDICTED, "SymptomPrediction", 1L);
        createAudit(patientArjun.getUser(), AuditAction.PRESCRIPTION_VIEWED, "Prescription", arjunPrescription.getId());

        log.info("Enriched demo data seeded. Password for all demo users: {}", DEMO_PASSWORD);
        log.info("Demo admin: admin@medflow.demo");
        log.info(
                "Demo doctors: doctor.asha@medflow.demo, doctor.rohan@medflow.demo, doctor.sara@medflow.demo, doctor.vikram@medflow.demo, doctor.leela@medflow.demo"
        );
        log.info(
                "Demo patients: patient.priya@medflow.demo, patient.arjun@medflow.demo, patient.meera@medflow.demo, patient.omar@medflow.demo, patient.kavya@medflow.demo, patient.tara@medflow.demo"
        );
    }

    private User createOrUpdateUser(String email, Role role, boolean enabled) {
        User user = userRepository.findByEmail(email).orElseGet(User::new);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        user.setRole(role);
        user.setEnabled(enabled);
        return userRepository.save(user);
    }

    private Doctor createOrUpdateDoctor(
            String email,
            String firstName,
            String lastName,
            String specialization,
            String licenseNumber,
            boolean active
    ) {
        User user = createOrUpdateUser(email, Role.DOCTOR, active);
        Doctor doctor = doctorRepository.findByUserId(user.getId()).orElseGet(Doctor::new);
        doctor.setUser(user);
        doctor.setFirstName(firstName);
        doctor.setLastName(lastName);
        doctor.setSpecialization(specialization);
        doctor.setLicenseNumber(licenseNumber);
        doctor.setActive(active);
        return doctorRepository.save(doctor);
    }

    private Patient createOrUpdatePatient(
            String email,
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            Gender gender,
            String phone,
            String address,
            String emergencyContact
    ) {
        User user = createOrUpdateUser(email, Role.PATIENT, true);
        Patient patient = patientRepository.findByUserId(user.getId()).orElseGet(Patient::new);
        patient.setUser(user);
        patient.setFirstName(firstName);
        patient.setLastName(lastName);
        patient.setDateOfBirth(dateOfBirth);
        patient.setGender(gender);
        patient.setPhone(phone);
        patient.setAddress(address);
        patient.setEmergencyContact(emergencyContact);
        return patientRepository.save(patient);
    }

    private Appointment createAppointment(
            Patient patient,
            Doctor doctor,
            LocalDateTime appointmentDateTime,
            AppointmentStatus status,
            String reason
    ) {
        if (status == AppointmentStatus.SCHEDULED
                && appointmentRepository.existsByDoctorIdAndAppointmentDateTimeAndStatus(
                doctor.getId(),
                appointmentDateTime,
                AppointmentStatus.SCHEDULED
        )) {
            return appointmentRepository
                    .findByDoctorIdAndStatusAndAppointmentDateTimeGreaterThanEqualAndAppointmentDateTimeLessThan(
                            doctor.getId(),
                            AppointmentStatus.SCHEDULED,
                            appointmentDateTime,
                            appointmentDateTime.plusMinutes(Appointment.SLOT_DURATION_MINUTES)
                    )
                    .stream()
                    .filter(appointment -> appointmentDateTime.equals(appointment.getAppointmentDateTime()))
                    .findFirst()
                    .orElseThrow();
        }

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDateTime(appointmentDateTime);
        appointment.setStatus(status);
        appointment.setReason(reason);
        return appointmentRepository.save(appointment);
    }

    private Encounter createEncounter(Appointment appointment, String chiefComplaint, String notes) {
        Encounter encounter = new Encounter();
        encounter.setAppointment(appointment);
        encounter.setPatient(appointment.getPatient());
        encounter.setDoctor(appointment.getDoctor());
        encounter.setVisitDate(appointment.getAppointmentDateTime());
        encounter.setChiefComplaint(chiefComplaint);
        encounter.setNotes(notes);
        return encounterRepository.save(encounter);
    }

    private Diagnosis createDiagnosis(
            Encounter encounter,
            String diagnosisCode,
            String diagnosisName,
            String description
    ) {
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setEncounter(encounter);
        diagnosis.setDiagnosisCode(diagnosisCode);
        diagnosis.setDiagnosisName(diagnosisName);
        diagnosis.setDescription(description);
        return diagnosisRepository.save(diagnosis);
    }

    private Prescription createPrescription(
            Encounter encounter,
            String medicationName,
            String dosage,
            String frequency,
            String duration,
            String instructions
    ) {
        Prescription prescription = new Prescription();
        prescription.setEncounter(encounter);
        prescription.setMedicationName(medicationName);
        prescription.setDosage(dosage);
        prescription.setFrequency(frequency);
        prescription.setDuration(duration);
        prescription.setInstructions(instructions);
        return prescriptionRepository.save(prescription);
    }

    private void createAudit(User user, AuditAction action, String entityType, Long entityId) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUser(user);
        auditLog.setAction(action.name());
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLogRepository.save(auditLog);
    }
}
