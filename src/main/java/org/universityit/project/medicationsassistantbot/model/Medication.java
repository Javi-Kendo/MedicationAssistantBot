package org.universityit.project.medicationsassistantbot.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.sql.Timestamp;

@Entity(name = "medicationsDataTable")
public class Medication {

    @Id
    private Long medicationId;

    private String name;

    private boolean combined;

    private double dosage;

    private Timestamp medicationAddedAt;

    public Long getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(Long medicationId) {
        this.medicationId = medicationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isCombined() {
        return combined;
    }

    public void setCombined(boolean combined) {
        this.combined = combined;
    }

    public double getDosage() {
        return dosage;
    }

    public void setDosage(double dosage) {
        this.dosage = dosage;
    }

    public Timestamp getMedicationAddedAt() {
        return medicationAddedAt;
    }

    public void setMedicationAddedAt(Timestamp medicationAddedAt) {
        this.medicationAddedAt = medicationAddedAt;
    }

    @Override
    public String toString() {
        return "Medication{" +
                "medicationId=" + medicationId +
                ", name='" + name + '\'' +
                ", combined=" + combined +
                ", dosage=" + dosage +
                ", medicationAddedAt=" + medicationAddedAt +
                '}';
    }
}
