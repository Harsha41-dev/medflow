package com.medflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "doctors",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_doctors_user", columnNames = "user_id"),
                @UniqueConstraint(name = "uk_doctors_license_number", columnNames = "license_number")
        },
        indexes = {
                @Index(name = "idx_doctors_specialization", columnList = "specialization"),
                @Index(name = "idx_doctors_active", columnList = "active")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_doctors_user"))
    private User user;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 100)
    private String specialization;

    @Column(name = "license_number", nullable = false, length = 100)
    private String licenseNumber;

    @Column(nullable = false)
    private boolean active = true;
}
