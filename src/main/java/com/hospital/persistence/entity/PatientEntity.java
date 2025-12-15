package com.hospital.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "patient")
public class PatientEntity {

    @Id
    @Column(name = "patient_id", nullable = false, length = 64)
    private String patientId;

    @Column(name = "discharged", nullable = false)
    private boolean discharged;

    @Column(name = "discharged_at")
    private Instant dischargedAt;

    @Column(name = "discharge_operation_id")
    private UUID dischargeOperationId;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public boolean isDischarged() {
        return discharged;
    }

    public void setDischarged(boolean discharged) {
        this.discharged = discharged;
    }

    public Instant getDischargedAt() {
        return dischargedAt;
    }

    public void setDischargedAt(Instant dischargedAt) {
        this.dischargedAt = dischargedAt;
    }

    public UUID getDischargeOperationId() {
        return dischargeOperationId;
    }

    public void setDischargeOperationId(UUID dischargeOperationId) {
        this.dischargeOperationId = dischargeOperationId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
