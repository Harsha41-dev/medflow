package com.medflow.repository;

import com.medflow.entity.Doctor;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByUserId(Long userId);

    Page<Doctor> findByActiveTrue(Pageable pageable);

    Page<Doctor> findByActiveTrueAndSpecializationIgnoreCase(String specialization, Pageable pageable);

    boolean existsByLicenseNumber(String licenseNumber);
}
