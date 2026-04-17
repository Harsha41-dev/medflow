package com.medflow.service;

import com.medflow.dto.DiagnosisRequest;
import com.medflow.dto.DiagnosisResponse;
import com.medflow.entity.AuditAction;
import com.medflow.entity.Diagnosis;
import com.medflow.entity.Encounter;
import com.medflow.exception.ResourceNotFoundException;
import com.medflow.repository.DiagnosisRepository;
import com.medflow.repository.EncounterRepository;
import com.medflow.util.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiagnosisService {

    private final DiagnosisRepository diagnosisRepository;
    private final EncounterRepository encounterRepository;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;
    private final DtoMapper dtoMapper;

    @Transactional
    public DiagnosisResponse addDiagnosis(Long encounterId, DiagnosisRequest request, Authentication authentication) {
        Encounter encounter = encounterRepository.findById(encounterId)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter not found"));

        authorizationService.checkEncounterDoctorAccess(encounter, authentication);

        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setEncounter(encounter);
        diagnosis.setDiagnosisCode(request.diagnosisCode());
        diagnosis.setDiagnosisName(request.diagnosisName());
        diagnosis.setDescription(request.description());

        Diagnosis savedDiagnosis = diagnosisRepository.save(diagnosis);
        auditService.record(authentication, AuditAction.DIAGNOSIS_CREATED, "Diagnosis", savedDiagnosis.getId());
        return dtoMapper.toDiagnosisResponse(savedDiagnosis);
    }
}
