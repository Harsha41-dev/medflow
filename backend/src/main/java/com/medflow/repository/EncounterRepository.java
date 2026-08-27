package com.medflow.repository;

import com.medflow.entity.Encounter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EncounterRepository extends JpaRepository<Encounter, Long> {

    boolean existsByAppointmentId(Long appointmentId);

    Page<Encounter> findByPatientIdOrderByVisitDateDesc(Long patientId, Pageable pageable);
}
