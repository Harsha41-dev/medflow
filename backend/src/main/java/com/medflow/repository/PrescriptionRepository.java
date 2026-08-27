package com.medflow.repository;

import com.medflow.entity.Prescription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    Page<Prescription> findByEncounterPatientIdOrderByIdDesc(Long patientId, Pageable pageable);
}
