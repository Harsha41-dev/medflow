package com.medflow.service;

import com.medflow.dto.AppointmentResponse;
import com.medflow.dto.CreateDoctorRequest;
import com.medflow.dto.DoctorRequest;
import com.medflow.dto.DoctorResponse;
import com.medflow.entity.AuditAction;
import com.medflow.entity.Doctor;
import com.medflow.entity.Role;
import com.medflow.entity.User;
import com.medflow.exception.BadRequestException;
import com.medflow.exception.ResourceNotFoundException;
import com.medflow.repository.AppointmentRepository;
import com.medflow.repository.DoctorRepository;
import com.medflow.util.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;
    private final DtoMapper dtoMapper;

    public Page<DoctorResponse> getDoctors(Pageable pageable) {
        return doctorRepository.findAll(pageable).map(dtoMapper::toDoctorResponse);
    }

    public Page<DoctorResponse> getActiveDoctors(Pageable pageable) {
        return doctorRepository.findByActiveTrue(pageable).map(dtoMapper::toDoctorResponse);
    }

    public Page<DoctorResponse> getActiveDoctors(String specialization, Pageable pageable) {
        if (specialization == null || specialization.isBlank()) {
            return getActiveDoctors(pageable);
        }

        return doctorRepository
                .findByActiveTrueAndSpecializationIgnoreCase(specialization.trim(), pageable)
                .map(dtoMapper::toDoctorResponse);
    }

    public DoctorResponse getDoctor(Long id) {
        return dtoMapper.toDoctorResponse(getDoctorEntityById(id));
    }

    public DoctorResponse getDoctorForCurrentUser(Long id, Authentication authentication) {
        Doctor doctor = getDoctorEntityById(id);
        authorizationService.checkDoctorProfileAccess(doctor, authentication);
        return dtoMapper.toDoctorResponse(doctor);
    }

    public Doctor getDoctorEntityById(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
    }

    public Doctor getDoctorEntityByUserId(Long userId) {
        return doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
    }

    public Page<AppointmentResponse> getDoctorAppointments(Long doctorId, Pageable pageable) {
        getDoctorEntityById(doctorId);
        return appointmentRepository.findByDoctorIdOrderByAppointmentDateTimeDesc(doctorId, pageable)
                .map(dtoMapper::toAppointmentResponse);
    }

    @Transactional
    public DoctorResponse createDoctorProfile(User user, DoctorRequest request) {
        validateDoctorUser(user);
        String licenseNumber = normalizeLicenseNumber(request.licenseNumber());

        if (doctorRepository.findByUserId(user.getId()).isPresent()) {
            throw new BadRequestException("Doctor profile already exists for this user");
        }
        if (doctorRepository.existsByLicenseNumber(licenseNumber)) {
            throw new BadRequestException("License number is already registered");
        }

        Doctor doctor = new Doctor();
        doctor.setUser(user);
        copyRequestToDoctor(request, doctor, licenseNumber);
        return dtoMapper.toDoctorResponse(doctorRepository.save(doctor));
    }

    @Transactional
    public DoctorResponse createDoctorAccount(CreateDoctorRequest request, Authentication authentication) {
        userService.ensureEmailAvailable(request.email());
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = userService.createDoctorUser(request.email(), encodedPassword);

        DoctorRequest doctorRequest = new DoctorRequest(
                request.firstName(),
                request.lastName(),
                request.specialization(),
                request.licenseNumber(),
                true
        );

        DoctorResponse doctorResponse = createDoctorProfile(user, doctorRequest);
        auditService.record(authentication, AuditAction.DOCTOR_CREATED, "Doctor", doctorResponse.id());
        return doctorResponse;
    }

    @Transactional
    public DoctorResponse updateDoctor(Long id, DoctorRequest request) {
        Doctor doctor = getDoctorEntityById(id);
        String licenseNumber = normalizeLicenseNumber(request.licenseNumber());

        if (!doctor.getLicenseNumber().equals(licenseNumber)
                && doctorRepository.existsByLicenseNumber(licenseNumber)) {
            throw new BadRequestException("License number is already registered");
        }

        copyRequestToDoctor(request, doctor, licenseNumber);
        return dtoMapper.toDoctorResponse(doctorRepository.save(doctor));
    }

    private void validateDoctorUser(User user) {
        if (user == null) {
            throw new BadRequestException("User is required");
        }
        if (user.getRole() != Role.DOCTOR) {
            throw new BadRequestException("Doctor profile can only be created for DOCTOR users");
        }
    }

    private void copyRequestToDoctor(DoctorRequest request, Doctor doctor, String licenseNumber) {
        doctor.setFirstName(request.firstName());
        doctor.setLastName(request.lastName());
        doctor.setSpecialization(request.specialization());
        doctor.setLicenseNumber(licenseNumber);
        doctor.setActive(request.active());
    }

    private String normalizeLicenseNumber(String licenseNumber) {
        if (licenseNumber == null || licenseNumber.isBlank()) {
            throw new BadRequestException("License number is required");
        }
        return licenseNumber.trim().toUpperCase();
    }
}
