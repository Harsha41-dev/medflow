package com.medflow.service;

import com.medflow.entity.Appointment;
import com.medflow.entity.AppointmentStatus;
import com.medflow.entity.Doctor;
import com.medflow.entity.Encounter;
import com.medflow.entity.Patient;
import com.medflow.exception.UnauthorizedAccessException;
import com.medflow.repository.AppointmentRepository;
import com.medflow.repository.DoctorRepository;
import com.medflow.repository.PatientRepository;
import com.medflow.security.CustomUserDetails;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorizationService {

    private static final List<AppointmentStatus> CLINICAL_ACCESS_STATUSES = List.of(
            AppointmentStatus.SCHEDULED,
            AppointmentStatus.COMPLETED
    );

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    public void checkPatientProfileAccess(Long patientId, Authentication authentication) {
        if (hasRole(authentication, "ROLE_ADMIN")) {
            return;
        }

        if (!hasRole(authentication, "ROLE_PATIENT")) {
            throw new UnauthorizedAccessException("You are not allowed to access this patient profile");
        }

        Patient loggedInPatient = patientRepository.findByUserId(getCurrentUserId(authentication))
                .orElseThrow(() -> new UnauthorizedAccessException("Patient profile not found for logged-in user"));

        if (!loggedInPatient.getId().equals(patientId)) {
            throw new UnauthorizedAccessException("You are not allowed to access this patient profile");
        }
    }

    public void checkDoctorProfileAccess(Doctor doctor, Authentication authentication) {
        if (doctor.isActive() || hasRole(authentication, "ROLE_ADMIN")) {
            return;
        }

        if (hasRole(authentication, "ROLE_DOCTOR")) {
            Doctor loggedInDoctor = doctorRepository.findByUserId(getCurrentUserId(authentication))
                    .orElseThrow(() -> new UnauthorizedAccessException("Doctor profile not found for logged-in user"));

            if (loggedInDoctor.getId().equals(doctor.getId())) {
                return;
            }
        }

        throw new UnauthorizedAccessException("You are not allowed to access this doctor profile");
    }

    public void checkAppointmentAccess(Appointment appointment, Authentication authentication) {
        if (hasRole(authentication, "ROLE_ADMIN")) {
            return;
        }

        if (hasRole(authentication, "ROLE_PATIENT")) {
            Patient loggedInPatient = getCurrentPatient(authentication);
            if (appointment.getPatient().getId().equals(loggedInPatient.getId())) {
                return;
            }
        }

        if (hasRole(authentication, "ROLE_DOCTOR")) {
            Doctor loggedInDoctor = getCurrentDoctor(authentication);
            if (appointment.getDoctor().getId().equals(loggedInDoctor.getId())) {
                return;
            }
        }

        throw new UnauthorizedAccessException("You are not allowed to access this appointment");
    }

    public void checkAppointmentCancellationAccess(Appointment appointment, Authentication authentication) {
        if (!hasRole(authentication, "ROLE_PATIENT")) {
            throw new UnauthorizedAccessException("Only patients can cancel appointments in this workflow");
        }

        Patient loggedInPatient = getCurrentPatient(authentication);
        if (!appointment.getPatient().getId().equals(loggedInPatient.getId())) {
            throw new UnauthorizedAccessException("You are not allowed to cancel this appointment");
        }
    }

    public void checkEncounterCreationAccess(Appointment appointment, Authentication authentication) {
        Doctor loggedInDoctor = getCurrentDoctor(authentication);
        if (!appointment.getDoctor().getId().equals(loggedInDoctor.getId())) {
            throw new UnauthorizedAccessException("You are not allowed to create an encounter for this appointment");
        }
    }

    public void checkEncounterAccess(Encounter encounter, Authentication authentication) {
        if (hasRole(authentication, "ROLE_PATIENT")) {
            Patient loggedInPatient = getCurrentPatient(authentication);
            if (encounter.getPatient().getId().equals(loggedInPatient.getId())) {
                return;
            }
        }

        if (hasRole(authentication, "ROLE_DOCTOR")) {
            Doctor loggedInDoctor = getCurrentDoctor(authentication);
            if (doctorCanAccessPatient(loggedInDoctor.getId(), encounter.getPatient().getId())) {
                return;
            }
        }

        throw new UnauthorizedAccessException("You are not allowed to access this encounter");
    }

    public void checkPatientClinicalAccess(Long patientId, Authentication authentication) {
        if (hasRole(authentication, "ROLE_PATIENT")) {
            Patient loggedInPatient = getCurrentPatient(authentication);
            if (loggedInPatient.getId().equals(patientId)) {
                return;
            }
        }

        if (hasRole(authentication, "ROLE_DOCTOR")) {
            Doctor loggedInDoctor = getCurrentDoctor(authentication);
            if (doctorCanAccessPatient(loggedInDoctor.getId(), patientId)) {
                return;
            }
        }

        throw new UnauthorizedAccessException("You are not allowed to access this patient's clinical records");
    }

    public void checkEncounterDoctorAccess(Encounter encounter, Authentication authentication) {
        Doctor loggedInDoctor = getCurrentDoctor(authentication);
        if (!encounter.getDoctor().getId().equals(loggedInDoctor.getId())) {
            throw new UnauthorizedAccessException("You are not allowed to modify this encounter");
        }
    }

    public Patient getCurrentPatient(Authentication authentication) {
        return patientRepository.findByUserId(getCurrentUserId(authentication))
                .orElseThrow(() -> new UnauthorizedAccessException("Patient profile not found for logged-in user"));
    }

    public Doctor getCurrentDoctor(Authentication authentication) {
        return doctorRepository.findByUserId(getCurrentUserId(authentication))
                .orElseThrow(() -> new UnauthorizedAccessException("Doctor profile not found for logged-in user"));
    }

    public Long getCurrentUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getId();
        }

        throw new UnauthorizedAccessException("Authenticated user details not found");
    }

    public boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }

    private boolean doctorCanAccessPatient(Long doctorId, Long patientId) {
        return appointmentRepository.existsByDoctorIdAndPatientIdAndStatusIn(
                doctorId,
                patientId,
                CLINICAL_ACCESS_STATUSES
        );
    }
}
