package com.medflow.repository;

import com.medflow.entity.Appointment;
import com.medflow.entity.AppointmentStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Page<Appointment> findAllByOrderByAppointmentDateTimeDesc(Pageable pageable);

    Page<Appointment> findByStatusOrderByAppointmentDateTimeDesc(
            AppointmentStatus status,
            Pageable pageable
    );

    Page<Appointment> findByPatientIdOrderByAppointmentDateTimeDesc(Long patientId, Pageable pageable);

    Page<Appointment> findByPatientIdAndStatusOrderByAppointmentDateTimeDesc(
            Long patientId,
            AppointmentStatus status,
            Pageable pageable
    );

    Page<Appointment> findByDoctorIdOrderByAppointmentDateTimeDesc(Long doctorId, Pageable pageable);

    Page<Appointment> findByDoctorIdAndStatusOrderByAppointmentDateTimeDesc(
            Long doctorId,
            AppointmentStatus status,
            Pageable pageable
    );

    boolean existsByDoctorIdAndAppointmentDateTimeAndStatus(
            Long doctorId,
            LocalDateTime appointmentDateTime,
            AppointmentStatus status
    );

    List<Appointment> findByDoctorIdAndStatusAndAppointmentDateTimeGreaterThanEqualAndAppointmentDateTimeLessThan(
            Long doctorId,
            AppointmentStatus status,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    boolean existsByDoctorIdAndPatientIdAndStatusIn(
            Long doctorId,
            Long patientId,
            Collection<AppointmentStatus> statuses
    );
}
