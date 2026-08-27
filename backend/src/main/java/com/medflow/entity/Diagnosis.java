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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "diagnoses",
        indexes = {
                @Index(name = "idx_diagnoses_encounter_id", columnList = "encounter_id"),
                @Index(name = "idx_diagnoses_diagnosis_code", columnList = "diagnosis_code")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Diagnosis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encounter_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_diagnoses_encounter"))
    private Encounter encounter;

    @Column(name = "diagnosis_code", nullable = false, length = 50)
    private String diagnosisCode;

    @Column(name = "diagnosis_name", nullable = false, length = 150)
    private String diagnosisName;

    @Column(length = 1000)
    private String description;
}
