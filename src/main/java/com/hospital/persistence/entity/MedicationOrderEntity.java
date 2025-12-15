package com.hospital.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "medication_order")
public class MedicationOrderEntity {

    @Id
    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "patient_id", nullable = false, length = 64)
    private String patientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MedicationOrderStatus status = MedicationOrderStatus.DRAFT;

    @Column(name = "signed_by", length = 64)
    private String signedBy;

    @Column(name = "signed_at")
    private Instant signedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public MedicationOrderStatus getStatus() {
        return status;
    }

    public void setStatus(MedicationOrderStatus status) {
        this.status = status;
    }

    public String getSignedBy() {
        return signedBy;
    }

    public void setSignedBy(String signedBy) {
        this.signedBy = signedBy;
    }

    public Instant getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(Instant signedAt) {
        this.signedAt = signedAt;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
