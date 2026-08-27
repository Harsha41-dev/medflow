package com.medflow.service;

import com.medflow.dto.AuthResponse;
import com.medflow.dto.LoginRequest;
import com.medflow.dto.PatientRequest;
import com.medflow.dto.PatientResponse;
import com.medflow.dto.RegisterRequest;
import com.medflow.entity.AuditAction;
import com.medflow.entity.User;
import com.medflow.exception.ResourceNotFoundException;
import com.medflow.security.CustomUserDetails;
import com.medflow.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final JwtService jwtService;
    private final AuditService auditService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        userService.ensureEmailAvailable(request.email());
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = userService.createPatientUser(request.email(), encodedPassword);
        PatientResponse patient = patientService.createPatientProfile(user, toPatientRequest(request));

        return createAuthResponse(CustomUserDetails.from(user), patient.id());
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            AuthResponse response = createAuthResponse(userDetails, getProfileId(userDetails));
            auditService.record(authentication, AuditAction.USER_LOGIN_SUCCESS, "User", userDetails.getId());
            return response;
        } catch (AuthenticationException ex) {
            auditService.recordFailedLoginIfUserExists(email);
            throw ex;
        }
    }

    private AuthResponse createAuthResponse(CustomUserDetails userDetails, Long profileId) {
        String accessToken = jwtService.generateToken(userDetails);
        return new AuthResponse(
                accessToken,
                TOKEN_TYPE,
                userDetails.getId(),
                profileId,
                userDetails.getUsername(),
                userDetails.getRole()
        );
    }

    private Long getProfileId(CustomUserDetails userDetails) {
        try {
            return switch (userDetails.getRole()) {
                case PATIENT -> patientService.getPatientEntityByUserId(userDetails.getId()).getId();
                case DOCTOR -> doctorService.getDoctorEntityByUserId(userDetails.getId()).getId();
                case ADMIN -> null;
            };
        } catch (ResourceNotFoundException ex) {
            return null;
        }
    }

    private PatientRequest toPatientRequest(RegisterRequest request) {
        return new PatientRequest(
                request.firstName(),
                request.lastName(),
                request.dateOfBirth(),
                request.gender(),
                request.phone(),
                request.address(),
                request.emergencyContact()
        );
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase();
    }
}
