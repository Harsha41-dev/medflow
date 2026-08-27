package com.medflow.service;

import com.medflow.dto.UserResponse;
import com.medflow.entity.Role;
import com.medflow.entity.User;
import com.medflow.exception.BadRequestException;
import com.medflow.exception.ResourceNotFoundException;
import com.medflow.repository.UserRepository;
import com.medflow.util.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final DtoMapper dtoMapper;

    public Page<UserResponse> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(dtoMapper::toUserResponse);
    }

    public UserResponse getUser(Long id) {
        return dtoMapper.toUserResponse(getUserEntityById(id));
    }

    public User getUserEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public User getUserEntityByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public void ensureEmailAvailable(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("Email is already registered");
        }
    }

    @Transactional
    public User createPatientUser(String email, String encodedPassword) {
        return createUser(email, encodedPassword, Role.PATIENT);
    }

    @Transactional
    public User createDoctorUser(String email, String encodedPassword) {
        return createUser(email, encodedPassword, Role.DOCTOR);
    }

    @Transactional
    public UserResponse activateUser(Long id) {
        User user = getUserEntityById(id);
        user.setEnabled(true);
        return dtoMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse deactivateUser(Long id) {
        User user = getUserEntityById(id);
        user.setEnabled(false);
        return dtoMapper.toUserResponse(userRepository.save(user));
    }

    private User createUser(String email, String encodedPassword, Role role) {
        String normalizedEmail = normalizeEmail(email);
        if (encodedPassword == null || encodedPassword.isBlank()) {
            throw new BadRequestException("Password is required");
        }

        ensureEmailAvailable(normalizedEmail);

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPassword(encodedPassword);
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email is required");
        }
        return email.trim().toLowerCase();
    }
}
