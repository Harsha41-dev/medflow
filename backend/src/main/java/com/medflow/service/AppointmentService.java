package com.medflow.service;

import com.medflow.dto.AppointmentRequest;
import com.medflow.dto.AppointmentResponse;
import com.medflow.dto.AppointmentSlotResponse;
import com.medflow.entity.Appointment;
import com.medflow.entity.AuditAction;
import com.medflow.entity.AppointmentStatus;
import com.medflow.entity.Doctor;
import com.medflow.entity.Patient;
import com.medflow.exception.AppointmentConflictException;
import com.medflow.exception.BadRequestException;
import com.medflow.exception.ResourceNotFoundException;
import com.medflow.repository.AppointmentRepository;
import com.medflow.repository.DoctorRepository;
import com.medflow.util.DtoMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
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
public class AppointmentService {

    private static final LocalTime CLINIC_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime CLINIC_END_TIME = LocalTime.of(17, 0);
    private static final DateTimeFormatter SLOT_DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;
    private final DtoMapper dtoMapper;

    public Page<AppointmentResponse> getAppointments(AppointmentStatus status, Pageable pageable, Authentication authentication) {
        if (authorizationService.hasRole(authentication, "ROLE_ADMIN")) {
            return getAdminAppointments(status, pageable);
        }

        if (authorizationService.hasRole(authentication, "ROLE_PATIENT")) {
            Patient patient = authorizationService.getCurrentPatient(authentication);
            return getPatientAppointments(patient.getId(), status, pageable);
        }

        if (authorizationService.hasRole(authentication, "ROLE_DOCTOR")) {
            Doctor doctor = authorizationService.getCurrentDoctor(authentication);
            return getDoctorAppointments(doctor.getId(), status, pageable);
        }

        throw new BadRequestException("Unsupported appointment listing role");
    }

    public List<AppointmentSlotResponse> getAvailableSlots(Long doctorId, LocalDate date) {
        if (doctorId == null) {
            throw new BadRequestException("Doctor is required");
        }

        if (date == null) {
            throw new BadRequestException("Appointment date is required");
        }

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        if (!doctor.isActive()) {
            throw new BadRequestException("Doctor is not active");
        }

        if (date.isBefore(LocalDate.now())) {
            throw new BadRequestException("Appointment date must be today or a future date");
        }

        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime = date.plusDays(1).atStartOfDay();
        Set<LocalDateTime> bookedSlots = appointmentRepository
                .findByDoctorIdAndStatusAndAppointmentDateTimeGreaterThanEqualAndAppointmentDateTimeLessThan(
                        doctorId,
                        AppointmentStatus.SCHEDULED,
                        startDateTime,
                        endDateTime
                )
                .stream()
                .map(Appointment::getAppointmentDateTime)
                .collect(Collectors.toSet());

        LocalDateTime now = LocalDateTime.now();

        List<AppointmentSlotResponse> availableSlots = new ArrayList<>();
        for (LocalTime slotTime = CLINIC_START_TIME;
                !slotTime.plusMinutes(Appointment.SLOT_DURATION_MINUTES).isAfter(CLINIC_END_TIME);
                slotTime = slotTime.plusMinutes(Appointment.SLOT_DURATION_MINUTES)) {
            LocalDateTime slotDateTime = date.atTime(slotTime);
            if (slotDateTime.isAfter(now) && !bookedSlots.contains(slotDateTime)) {
                availableSlots.add(new AppointmentSlotResponse(
                        slotDateTime,
                        slotTime.format(SLOT_DISPLAY_FORMATTER)
                ));
            }
        }

        return availableSlots;
    }

    @Transactional
    public AppointmentResponse getAppointment(Long id, Authentication authentication) {
        Appointment appointment = getAppointmentEntityById(id);
        authorizationService.checkAppointmentAccess(appointment, authentication);
        auditService.record(authentication, AuditAction.APPOINTMENT_VIEWED, "Appointment", appointment.getId());
        return dtoMapper.toAppointmentResponse(appointment);
    }

    public Appointment getAppointmentEntityById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
    }

    @Transactional
    public AppointmentResponse bookAppointment(AppointmentRequest request, Authentication authentication) {
        Patient patient = authorizationService.getCurrentPatient(authentication);
        Doctor doctor = doctorRepository.findById(request.doctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        if (!doctor.isActive()) {
            throw new BadRequestException("Doctor is not active");
        }

        validateFutureAppointment(request.appointmentDateTime());
        validateFixedSlot(request.appointmentDateTime());
        ensureScheduledSlotAvailable(doctor.getId(), request.appointmentDateTime());

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDateTime(request.appointmentDateTime());
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setReason(request.reason());

        try {
            Appointment savedAppointment = appointmentRepository.saveAndFlush(appointment);
            auditService.record(
                    authentication,
                    AuditAction.APPOINTMENT_CREATED,
                    "Appointment",
                    savedAppointment.getId()
            );
            return dtoMapper.toAppointmentResponse(savedAppointment);
        } catch (DataIntegrityViolationException ex) {
            if (isScheduledSlotConstraintViolation(ex)) {
                throw new AppointmentConflictException("Doctor already has a scheduled appointment at this time");
            }
            throw ex;
        }
    }

    @Transactional
    public AppointmentResponse cancelAppointment(Long id, Authentication authentication) {
        Appointment appointment = getAppointmentEntityById(id);
        authorizationService.checkAppointmentCancellationAccess(appointment, authentication);

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new BadRequestException("Only scheduled appointments can be cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment savedAppointment = appointmentRepository.save(appointment);
        auditService.record(
                authentication,
                AuditAction.APPOINTMENT_CANCELLED,
                "Appointment",
                savedAppointment.getId()
        );
        return dtoMapper.toAppointmentResponse(savedAppointment);
    }

    public boolean isScheduledSlotTaken(Long doctorId, LocalDateTime appointmentDateTime) {
        return appointmentRepository.existsByDoctorIdAndAppointmentDateTimeAndStatus(
                doctorId,
                appointmentDateTime,
                AppointmentStatus.SCHEDULED
        );
    }

    public void validateFutureAppointment(LocalDateTime appointmentDateTime) {
        if (appointmentDateTime == null) {
            throw new BadRequestException("Appointment date and time is required");
        }
        if (!appointmentDateTime.isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Appointment must be in the future");
        }
    }

    public void validateFixedSlot(LocalDateTime appointmentDateTime) {
        if (appointmentDateTime == null) {
            throw new BadRequestException("Appointment date and time is required");
        }
        if (appointmentDateTime.getSecond() != 0 || appointmentDateTime.getNano() != 0) {
            throw new BadRequestException("Appointment time must not include seconds");
        }
        if (appointmentDateTime.getMinute() % Appointment.SLOT_DURATION_MINUTES != 0) {
            throw new BadRequestException("Appointments must use 30-minute slots");
        }
    }

    public void ensureScheduledSlotAvailable(Long doctorId, LocalDateTime appointmentDateTime) {
        if (isScheduledSlotTaken(doctorId, appointmentDateTime)) {
            throw new AppointmentConflictException("Doctor already has a scheduled appointment at this time");
        }
    }

    private Page<AppointmentResponse> getAdminAppointments(AppointmentStatus status, Pageable pageable) {
        Page<Appointment> appointments = status == null
                ? appointmentRepository.findAllByOrderByAppointmentDateTimeDesc(pageable)
                : appointmentRepository.findByStatusOrderByAppointmentDateTimeDesc(status, pageable);

        return appointments.map(dtoMapper::toAppointmentResponse);
    }

    private Page<AppointmentResponse> getPatientAppointments(Long patientId, AppointmentStatus status, Pageable pageable) {
        Page<Appointment> appointments = status == null
                ? appointmentRepository.findByPatientIdOrderByAppointmentDateTimeDesc(patientId, pageable)
                : appointmentRepository.findByPatientIdAndStatusOrderByAppointmentDateTimeDesc(patientId, status, pageable);

        return appointments.map(dtoMapper::toAppointmentResponse);
    }

    private Page<AppointmentResponse> getDoctorAppointments(Long doctorId, AppointmentStatus status, Pageable pageable) {
        Page<Appointment> appointments = status == null
                ? appointmentRepository.findByDoctorIdOrderByAppointmentDateTimeDesc(doctorId, pageable)
                : appointmentRepository.findByDoctorIdAndStatusOrderByAppointmentDateTimeDesc(doctorId, status, pageable);

        return appointments.map(dtoMapper::toAppointmentResponse);
    }

    private boolean isScheduledSlotConstraintViolation(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        if (message == null) {
            return false;
        }

        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("ux_appointments_scheduled_doctor_time")
                || lowerMessage.contains("uk_appointments_doctor_time");
    }
}
