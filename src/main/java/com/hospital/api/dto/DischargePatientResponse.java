package com.hospital.api.dto;

import java.time.Instant;

public class DischargePatientResponse {

    private String patientId;
    private boolean discharged;
    private Instant dischargedAt;

    public DischargePatientResponse() {
    }

    public DischargePatientResponse(String patientId, boolean discharged, Instant dischargedAt) {
        this.patientId = patientId;
        this.discharged = discharged;
        this.dischargedAt = dischargedAt;
    }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public boolean isDischarged() { return discharged; }
    public void setDischarged(boolean discharged) { this.discharged = discharged; }

    public Instant getDischargedAt() { return dischargedAt; }
    public void setDischargedAt(Instant dischargedAt) { this.dischargedAt = dischargedAt; }
}
