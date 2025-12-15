package com.hospital.api.dto;

public class SignMedicationOrderRequest {

    private String clinicianId;

    public SignMedicationOrderRequest() {
    }

    public SignMedicationOrderRequest(String clinicianId) {
        this.clinicianId = clinicianId;
    }

    public String getClinicianId() { return clinicianId; }
    public void setClinicianId(String clinicianId) { this.clinicianId = clinicianId; }
}
