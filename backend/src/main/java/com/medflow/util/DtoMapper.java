package com.medflow.util;

import com.medflow.dto.AppointmentResponse;
import com.medflow.dto.AuditLogResponse;
import com.medflow.dto.DiagnosisResponse;
import com.medflow.dto.DoctorResponse;
import com.medflow.dto.EncounterResponse;
import com.medflow.dto.PatientResponse;
import com.medflow.dto.PrescriptionResponse;
import com.medflow.dto.UserResponse;
import com.medflow.entity.Appointment;
import com.medflow.entity.AuditLog;
import com.medflow.entity.Diagnosis;
import com.medflow.entity.Doctor;
import com.medflow.entity.Encounter;
import com.medflow.entity.Patient;
import com.medflow.entity.Prescription;
import com.medflow.entity.User;
import org.springframework.stereotype.Component;

@Component
public class DtoMapper {

    public UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public PatientResponse toPatientResponse(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getDateOfBirth(),
                patient.getGender(),
                patient.getPhone(),
                patient.getAddress(),
                patient.getEmergencyContact()
        );
    }

    public DoctorResponse toDoctorResponse(Doctor doctor) {
        User user = doctor.getUser();
        return new DoctorResponse(
                doctor.getId(),
                user.getId(),
                user.getEmail(),
                doctor.getFirstName(),
                doctor.getLastName(),
                doctor.getSpecialization(),
                doctor.getLicenseNumber(),
                doctor.isActive()
        );
    }

    public AppointmentResponse toAppointmentResponse(Appointment appointment) {
        Patient patient = appointment.getPatient();
        Doctor doctor = appointment.getDoctor();
        return new AppointmentResponse(
                appointment.getId(),
                patient.getId(),
                fullName(patient.getFirstName(), patient.getLastName()),
                doctor.getId(),
                fullName(doctor.getFirstName(), doctor.getLastName()),
                doctor.getSpecialization(),
                appointment.getAppointmentDateTime(),
                appointment.getStatus(),
                appointment.getReason(),
                appointment.getCreatedAt()
        );
    }

    public EncounterResponse toEncounterResponse(Encounter encounter) {
        Patient patient = encounter.getPatient();
        Doctor doctor = encounter.getDoctor();
        return new EncounterResponse(
                encounter.getId(),
                encounter.getAppointment().getId(),
                patient.getId(),
                fullName(patient.getFirstName(), patient.getLastName()),
                doctor.getId(),
                fullName(doctor.getFirstName(), doctor.getLastName()),
                encounter.getVisitDate(),
                encounter.getChiefComplaint(),
                encounter.getNotes()
        );
    }

    public DiagnosisResponse toDiagnosisResponse(Diagnosis diagnosis) {
        return new DiagnosisResponse(
                diagnosis.getId(),
                diagnosis.getEncounter().getId(),
                diagnosis.getDiagnosisCode(),
                diagnosis.getDiagnosisName(),
                diagnosis.getDescription()
        );
    }

    public PrescriptionResponse toPrescriptionResponse(Prescription prescription) {
        return new PrescriptionResponse(
                prescription.getId(),
                prescription.getEncounter().getId(),
                prescription.getMedicationName(),
                prescription.getDosage(),
                prescription.getFrequency(),
                prescription.getDuration(),
                prescription.getInstructions()
        );
    }

    public AuditLogResponse toAuditLogResponse(AuditLog auditLog) {
        User user = auditLog.getUser();
        return new AuditLogResponse(
                auditLog.getId(),
                user.getId(),
                user.getEmail(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getTimestamp()
        );
    }

    private String fullName(String firstName, String lastName) {
        return firstName + " " + lastName;
    }
}
