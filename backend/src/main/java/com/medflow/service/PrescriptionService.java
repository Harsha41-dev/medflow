package com.medflow.service;

import com.medflow.dto.PrescriptionRequest;
import com.medflow.dto.PrescriptionResponse;
import com.medflow.entity.AuditAction;
import com.medflow.entity.Encounter;
import com.medflow.entity.Prescription;
import com.medflow.exception.ResourceNotFoundException;
import com.medflow.repository.EncounterRepository;
import com.medflow.repository.PrescriptionRepository;
import com.medflow.util.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final EncounterRepository encounterRepository;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;
    private final DtoMapper dtoMapper;

    @Transactional
    public Page<PrescriptionResponse> getPatientPrescriptions(
            Long patientId,
            Pageable pageable,
            Authentication authentication
    ) {
        authorizationService.checkPatientClinicalAccess(patientId, authentication);
        auditService.record(authentication, AuditAction.PRESCRIPTION_VIEWED, "Patient", patientId);
        return prescriptionRepository.findByEncounterPatientIdOrderByIdDesc(patientId, pageable)
                .map(dtoMapper::toPrescriptionResponse);
    }

    @Transactional
    public PrescriptionResponse addPrescription(
            Long encounterId,
            PrescriptionRequest request,
            Authentication authentication
    ) {
        Encounter encounter = encounterRepository.findById(encounterId)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter not found"));

        authorizationService.checkEncounterDoctorAccess(encounter, authentication);

        Prescription prescription = new Prescription();
        prescription.setEncounter(encounter);
        prescription.setMedicationName(request.medicationName());
        prescription.setDosage(request.dosage());
        prescription.setFrequency(request.frequency());
        prescription.setDuration(request.duration());
        prescription.setInstructions(request.instructions());

        Prescription savedPrescription = prescriptionRepository.save(prescription);
        auditService.record(
                authentication,
                AuditAction.PRESCRIPTION_CREATED,
                "Prescription",
                savedPrescription.getId()
        );
        return dtoMapper.toPrescriptionResponse(savedPrescription);
    }
}
