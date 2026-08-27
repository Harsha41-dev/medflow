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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "encounters",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_encounters_appointment", columnNames = "appointment_id")
        },
        indexes = {
                @Index(name = "idx_encounters_patient_id", columnList = "patient_id"),
                @Index(name = "idx_encounters_doctor_id", columnList = "doctor_id"),
                @Index(name = "idx_encounters_visit_date", columnList = "visit_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Encounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_encounters_appointment"))
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_encounters_patient"))
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_encounters_doctor"))
    private Doctor doctor;

    @Column(name = "visit_date", nullable = false)
    private LocalDateTime visitDate;

    @Column(name = "chief_complaint", nullable = false, length = 500)
    private String chiefComplaint;

    @Column(length = 2000)
    private String notes;
}
