package com.medflow.service;

import com.medflow.dto.EncounterRequest;
import com.medflow.dto.EncounterResponse;
import com.medflow.entity.Appointment;
import com.medflow.entity.AuditAction;
import com.medflow.entity.AppointmentStatus;
import com.medflow.entity.Encounter;
import com.medflow.exception.AppointmentConflictException;
import com.medflow.exception.BadRequestException;
import com.medflow.exception.ResourceNotFoundException;
import com.medflow.repository.AppointmentRepository;
import com.medflow.repository.EncounterRepository;
import com.medflow.util.DtoMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EncounterService {

    private final EncounterRepository encounterRepository;
    private final AppointmentRepository appointmentRepository;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;
    private final DtoMapper dtoMapper;

    @Transactional
    public EncounterResponse getEncounter(Long id, Authentication authentication) {
        Encounter encounter = getEncounterEntityById(id);
        authorizationService.checkEncounterAccess(encounter, authentication);
        auditService.record(authentication, AuditAction.ENCOUNTER_VIEWED, "Encounter", encounter.getId());
        return dtoMapper.toEncounterResponse(encounter);
    }

    public Encounter getEncounterEntityById(Long id) {
        return encounterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter not found"));
    }

    public Page<EncounterResponse> getPatientEncounters(Long patientId, Pageable pageable, Authentication authentication) {
        authorizationService.checkPatientClinicalAccess(patientId, authentication);
        return encounterRepository.findByPatientIdOrderByVisitDateDesc(patientId, pageable)
                .map(dtoMapper::toEncounterResponse);
    }

    @Transactional
    public EncounterResponse createEncounter(EncounterRequest request, Authentication authentication) {
        Appointment appointment = appointmentRepository.findById(request.appointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        authorizationService.checkEncounterCreationAccess(appointment, authentication);

        if (encounterRepository.existsByAppointmentId(appointment.getId())) {
            throw new AppointmentConflictException("Encounter already exists for this appointment");
        }

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new BadRequestException("Encounter can only be created for a scheduled appointment");
        }

        Encounter encounter = new Encounter();
        encounter.setAppointment(appointment);
        encounter.setPatient(appointment.getPatient());
        encounter.setDoctor(appointment.getDoctor());
        encounter.setVisitDate(LocalDateTime.now());
        encounter.setChiefComplaint(request.chiefComplaint());
        encounter.setNotes(request.notes());

        appointment.setStatus(AppointmentStatus.COMPLETED);

        try {
            Encounter savedEncounter = encounterRepository.saveAndFlush(encounter);
            auditService.record(authentication, AuditAction.ENCOUNTER_CREATED, "Encounter", savedEncounter.getId());
            return dtoMapper.toEncounterResponse(savedEncounter);
        } catch (DataIntegrityViolationException ex) {
            if (isEncounterAppointmentConstraintViolation(ex)) {
                throw new AppointmentConflictException("Encounter already exists for this appointment");
            }
            throw ex;
        }
    }

    private boolean isEncounterAppointmentConstraintViolation(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        if (message == null) {
            return false;
        }

        return message.toLowerCase().contains("uk_encounters_appointment");
    }
}
