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
}
