package com.medflow.service;

import com.medflow.dto.PatientRequest;
import com.medflow.dto.PatientResponse;
import com.medflow.dto.PatientUpdateRequest;
import com.medflow.entity.AuditAction;
import com.medflow.entity.Patient;
import com.medflow.entity.Role;
import com.medflow.entity.User;
import com.medflow.exception.BadRequestException;
import com.medflow.exception.ResourceNotFoundException;
import com.medflow.repository.PatientRepository;
import com.medflow.util.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientService {

    private final PatientRepository patientRepository;
    private final AuditService auditService;
    private final DtoMapper dtoMapper;

    @Transactional
    public PatientResponse getPatient(Long id, Authentication authentication) {
        Patient patient = getPatientEntityById(id);
        auditService.record(authentication, AuditAction.PATIENT_VIEWED, "Patient", patient.getId());
        return dtoMapper.toPatientResponse(patient);
    }

    public Patient getPatientEntityById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
    }

    public Patient getPatientEntityByUserId(Long userId) {
        return patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
    }

    @Transactional
    public PatientResponse createPatientProfile(User user, PatientRequest request) {
        validatePatientUser(user);
        if (patientRepository.existsByUserId(user.getId())) {
            throw new BadRequestException("Patient profile already exists for this user");
        }

        Patient patient = new Patient();
        patient.setUser(user);
        copyRequestToPatient(request, patient);
        return dtoMapper.toPatientResponse(patientRepository.save(patient));
    }

    @Transactional
    public PatientResponse updatePatient(Long id, PatientUpdateRequest request, Authentication authentication) {
        Patient patient = getPatientEntityById(id);
        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setPhone(request.phone());
        patient.setAddress(request.address());
        patient.setEmergencyContact(request.emergencyContact());
        Patient savedPatient = patientRepository.save(patient);
        auditService.record(authentication, AuditAction.PATIENT_UPDATED, "Patient", savedPatient.getId());
        return dtoMapper.toPatientResponse(savedPatient);
    }

    private void validatePatientUser(User user) {
        if (user == null) {
            throw new BadRequestException("User is required");
        }
        if (user.getRole() != Role.PATIENT) {
            throw new BadRequestException("Patient profile can only be created for PATIENT users");
        }
    }

    private void copyRequestToPatient(PatientRequest request, Patient patient) {
        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setDateOfBirth(request.dateOfBirth());
        patient.setGender(request.gender());
        patient.setPhone(request.phone());
        patient.setAddress(request.address());
        patient.setEmergencyContact(request.emergencyContact());
    }
}
