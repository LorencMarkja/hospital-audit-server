package com.hospital.api.dto;

import com.hospital.persistence.entity.MedicationOrderStatus;

import java.time.Instant;

public class MedicationOrderResponse {

    private String orderId;
    private String patientId;
    private MedicationOrderStatus status;
    private String signedBy;
    private Instant signedAt;

    public MedicationOrderResponse() {
    }

    public MedicationOrderResponse(String orderId,
                                   String patientId,
                                   MedicationOrderStatus status,
                                   String signedBy,
                                   Instant signedAt) {
        this.orderId = orderId;
        this.patientId = patientId;
        this.status = status;
        this.signedBy = signedBy;
        this.signedAt = signedAt;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public MedicationOrderStatus getStatus() { return status; }
    public void setStatus(MedicationOrderStatus status) { this.status = status; }

    public String getSignedBy() { return signedBy; }
    public void setSignedBy(String signedBy) { this.signedBy = signedBy; }

    public Instant getSignedAt() { return signedAt; }
    public void setSignedAt(Instant signedAt) { this.signedAt = signedAt; }
}
