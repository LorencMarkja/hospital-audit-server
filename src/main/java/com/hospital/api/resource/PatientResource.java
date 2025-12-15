package com.hospital.api.resource;

import com.hospital.api.dto.DischargePatientResponse;
import com.hospital.persistence.entity.PatientEntity;
import com.hospital.persistence.entity.OperationType;
import com.hospital.service.OperationService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.Map;

@Path("/patients")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class PatientResource {

    @Inject
    OperationService operationService;

    @PersistenceContext(unitName = "hospitalPU")
    EntityManager em;

    @POST
    @Path("/{patientId}/discharge")
    public Response dischargePatient(
            @PathParam("patientId") String patientId,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Idempotency-Key header is required"))
                    .build();
        }

        String uri = "/v1/patients/" + patientId + "/discharge";
        String requestJson = "{}"; // no body for now

        return operationService.execute(
                OperationType.DISCHARGE_PATIENT,
                idempotencyKey,
                "POST",
                uri,
                requestJson,
                () -> dischargePatient(patientId)
        );
    }

    @Transactional
    protected DischargePatientResponse dischargePatient(String patientId) {
        Instant now = Instant.now();
        PatientEntity patient = em.find(PatientEntity.class, patientId);

        if (patient == null) {
            patient = new PatientEntity();
            patient.setPatientId(patientId);
            patient.setDischarged(true);
            patient.setDischargedAt(now);
            em.persist(patient);
        } else if (!patient.isDischarged()) {
            patient.setDischarged(true);
            patient.setDischargedAt(now);
        }
        // if already discharged, no-op (idempotent effect)

        return new DischargePatientResponse(
                patient.getPatientId(),
                patient.isDischarged(),
                patient.getDischargedAt()
        );
    }

    @POST
    @Path("/{patientId}/discharge/undo")
    public Response undoDischargePatient(
            @PathParam("patientId") String patientId,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Idempotency-Key header is required"))
                    .build();
        }

        String uri = "/v1/patients/" + patientId + "/discharge/undo";
        String requestJson = "{}";

        return operationService.execute(
                OperationType.UNDO_DISCHARGE_PATIENT,
                idempotencyKey,
                "POST",
                uri,
                requestJson,
                () -> undoDischarge(patientId)
        );
    }

    @Transactional
    protected DischargePatientResponse undoDischarge(String patientId) {
        PatientEntity patient = em.find(PatientEntity.class, patientId);

        if (patient == null) {
            // Nothing to undo, return "not discharged" baseline
            patient = new PatientEntity();
            patient.setPatientId(patientId);
            patient.setDischarged(false);
            patient.setDischargedAt(null);
            em.persist(patient);
        } else if (patient.isDischarged()) {
            // Undo discharge
            patient.setDischarged(false);
            patient.setDischargedAt(null);
        }
        // If already not discharged, no-op

        return new DischargePatientResponse(
                patient.getPatientId(),
                patient.isDischarged(),
                patient.getDischargedAt()
        );
    }
}
