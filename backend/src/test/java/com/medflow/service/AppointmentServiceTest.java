package com.medflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.medflow.dto.AppointmentRequest;
import com.medflow.dto.AppointmentResponse;
import com.medflow.dto.AppointmentSlotResponse;
import com.medflow.entity.Appointment;
import com.medflow.entity.AppointmentStatus;
import com.medflow.entity.AuditAction;
import com.medflow.entity.Doctor;
import com.medflow.entity.Patient;
import com.medflow.exception.AppointmentConflictException;
import com.medflow.exception.BadRequestException;
import com.medflow.repository.AppointmentRepository;
import com.medflow.repository.DoctorRepository;
import com.medflow.util.DtoMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    private static final String APPOINTMENT_REASON = "Skin irritation";

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private AuditService auditService;

    @Mock
    private DtoMapper dtoMapper;

    @InjectMocks
    private AppointmentService appointmentService;

    @Test
    void availableSlotsHideScheduledAppointments() {
        Long doctorId = 2L;
        Doctor doctor = doctorWithId(doctorId, true);
        LocalDate date = LocalDate.now().plusDays(7);
        LocalDateTime bookedTime = date.atTime(10, 0);
        Appointment bookedAppointment = new Appointment();
        bookedAppointment.setAppointmentDateTime(bookedTime);

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findByDoctorIdAndStatusAndAppointmentDateTimeGreaterThanEqualAndAppointmentDateTimeLessThan(
                doctorId,
                AppointmentStatus.SCHEDULED,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(bookedAppointment));

        List<AppointmentSlotResponse> slots = appointmentService.getAvailableSlots(doctorId, date);

        assertThat(slots).hasSize(15);
        assertThat(slots)
                .extracting(AppointmentSlotResponse::appointmentDateTime)
                .doesNotContain(bookedTime)
                .contains(date.atTime(10, 30));
    }

    @Test
    void availableSlotsRejectInactiveDoctor() {
        Long doctorId = 2L;
        Doctor inactiveDoctor = doctorWithId(doctorId, false);
        LocalDate date = LocalDate.now().plusDays(7);

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(inactiveDoctor));

        assertThatThrownBy(() -> appointmentService.getAvailableSlots(doctorId, date))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Doctor is not active");
    }

    @Test
    void activeDoctorAndValidSlotCreatesAppointment() {
        Authentication authentication = mock(Authentication.class);
        Patient patient = patientWithId(1L);
        Doctor doctor = doctorWithId(2L, true);
        LocalDateTime appointmentTime = futureSlot();
        AppointmentRequest request = new AppointmentRequest(doctor.getId(), appointmentTime, APPOINTMENT_REASON);
        AppointmentResponse expectedResponse = appointmentResponse(10L, patient.getId(), doctor.getId(), appointmentTime);
        ArgumentCaptor<Appointment> appointmentCaptor = ArgumentCaptor.forClass(Appointment.class);

        when(authorizationService.getCurrentPatient(authentication)).thenReturn(patient);
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateTimeAndStatus(
                doctor.getId(),
                appointmentTime,
                AppointmentStatus.SCHEDULED
        )).thenReturn(false);
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment appointment = invocation.getArgument(0);
            appointment.setId(10L);
            return appointment;
        });
        when(dtoMapper.toAppointmentResponse(any(Appointment.class))).thenReturn(expectedResponse);

        AppointmentResponse response = appointmentService.bookAppointment(request, authentication);

        assertThat(response).isSameAs(expectedResponse);
        verify(appointmentRepository).saveAndFlush(appointmentCaptor.capture());
        Appointment savedAppointment = appointmentCaptor.getValue();
        assertThat(savedAppointment.getPatient()).isEqualTo(patient);
        assertThat(savedAppointment.getDoctor()).isEqualTo(doctor);
        assertThat(savedAppointment.getAppointmentDateTime()).isEqualTo(appointmentTime);
        assertThat(savedAppointment.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(savedAppointment.getReason()).isEqualTo(APPOINTMENT_REASON);
        verify(auditService).record(authentication, AuditAction.APPOINTMENT_CREATED, "Appointment", 10L);
    }

    @Test
    void inactiveDoctorIsRejected() {
        Authentication authentication = mock(Authentication.class);
        Patient patient = patientWithId(1L);
        Doctor inactiveDoctor = doctorWithId(2L, false);
        AppointmentRequest request = new AppointmentRequest(inactiveDoctor.getId(), futureSlot(), APPOINTMENT_REASON);

        when(authorizationService.getCurrentPatient(authentication)).thenReturn(patient);
        when(doctorRepository.findById(inactiveDoctor.getId())).thenReturn(Optional.of(inactiveDoctor));

        assertThatThrownBy(() -> appointmentService.bookAppointment(request, authentication))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Doctor is not active");

        verify(appointmentRepository, never()).saveAndFlush(any(Appointment.class));
    }

    @Test
    void pastAppointmentTimeIsRejected() {
        Authentication authentication = mock(Authentication.class);
        Patient patient = patientWithId(1L);
        Doctor doctor = doctorWithId(2L, true);
        AppointmentRequest request = new AppointmentRequest(doctor.getId(), pastSlot(), APPOINTMENT_REASON);

        when(authorizationService.getCurrentPatient(authentication)).thenReturn(patient);
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));

        assertThatThrownBy(() -> appointmentService.bookAppointment(request, authentication))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Appointment must be in the future");

        verify(appointmentRepository, never()).saveAndFlush(any(Appointment.class));
    }

    @Test
    void invalidThirtyMinuteSlotIsRejected() {
        Authentication authentication = mock(Authentication.class);
        Patient patient = patientWithId(1L);
        Doctor doctor = doctorWithId(2L, true);
        AppointmentRequest request = new AppointmentRequest(doctor.getId(), invalidFutureSlot(), APPOINTMENT_REASON);

        when(authorizationService.getCurrentPatient(authentication)).thenReturn(patient);
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));

        assertThatThrownBy(() -> appointmentService.bookAppointment(request, authentication))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Appointments must use 30-minute slots");

        verify(appointmentRepository, never()).saveAndFlush(any(Appointment.class));
    }

    @Test
    void existingScheduledAppointmentCausesConflict() {
        Authentication authentication = mock(Authentication.class);
        Patient patient = patientWithId(1L);
        Doctor doctor = doctorWithId(2L, true);
        LocalDateTime appointmentTime = futureSlot();
        AppointmentRequest request = new AppointmentRequest(doctor.getId(), appointmentTime, APPOINTMENT_REASON);

        when(authorizationService.getCurrentPatient(authentication)).thenReturn(patient);
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateTimeAndStatus(
                doctor.getId(),
                appointmentTime,
                AppointmentStatus.SCHEDULED
        )).thenReturn(true);

        assertThatThrownBy(() -> appointmentService.bookAppointment(request, authentication))
                .isInstanceOf(AppointmentConflictException.class)
                .hasMessage("Doctor already has a scheduled appointment at this time");

        verify(appointmentRepository, never()).saveAndFlush(any(Appointment.class));
    }

    @Test
    void databaseUniqueViolationIsConvertedToAppointmentConflict() {
        Authentication authentication = mock(Authentication.class);
        Patient patient = patientWithId(1L);
        Doctor doctor = doctorWithId(2L, true);
        LocalDateTime appointmentTime = futureSlot();
        AppointmentRequest request = new AppointmentRequest(doctor.getId(), appointmentTime, APPOINTMENT_REASON);

        when(authorizationService.getCurrentPatient(authentication)).thenReturn(patient);
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateTimeAndStatus(
                doctor.getId(),
                appointmentTime,
                AppointmentStatus.SCHEDULED
        )).thenReturn(false);
        when(appointmentRepository.saveAndFlush(any(Appointment.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicate key value violates unique constraint ux_appointments_scheduled_doctor_time"
                ));

        assertThatThrownBy(() -> appointmentService.bookAppointment(request, authentication))
                .isInstanceOf(AppointmentConflictException.class)
                .hasMessage("Doctor already has a scheduled appointment at this time");
    }

    private Patient patientWithId(Long id) {
        Patient patient = new Patient();
        patient.setId(id);
        return patient;
    }

    private Doctor doctorWithId(Long id, boolean active) {
        Doctor doctor = new Doctor();
        doctor.setId(id);
        doctor.setActive(active);
        return doctor;
    }

    private AppointmentResponse appointmentResponse(
            Long id,
            Long patientId,
            Long doctorId,
            LocalDateTime appointmentTime
    ) {
        return new AppointmentResponse(
                id,
                patientId,
                "Aarav Sharma",
                doctorId,
                "Ananya Rao",
                "Dermatology",
                appointmentTime,
                AppointmentStatus.SCHEDULED,
                APPOINTMENT_REASON,
                LocalDateTime.now()
        );
    }

    private LocalDateTime futureSlot() {
        return LocalDateTime.now()
                .plusDays(30)
                .withHour(10)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }

    private LocalDateTime invalidFutureSlot() {
        return futureSlot().withMinute(10);
    }

    private LocalDateTime pastSlot() {
        return LocalDateTime.now()
                .minusDays(1)
                .withHour(10)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }
}
