package com.hospital.api.resource;

import com.hospital.api.dto.MedicationOrderResponse;
import com.hospital.api.dto.SignMedicationOrderRequest;
import com.hospital.persistence.entity.MedicationOrderEntity;
import com.hospital.persistence.entity.MedicationOrderStatus;
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

@Path("/patients/{patientId}/orders")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class MedicationOrderResource {

    @Inject
    OperationService operationService;

    @PersistenceContext(unitName = "hospitalPU")
    EntityManager em;


    @POST
    @Path("/{orderId}/sign")
    public Response signOrder(
            @PathParam("patientId") String patientId,
            @PathParam("orderId") String orderId,
            @HeaderParam("Idempotency-Key") String idempotencyKey,
            SignMedicationOrderRequest request
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Idempotency-Key header is required"))
                    .build();
        }

        if (request == null || request.getClinicianId() == null || request.getClinicianId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "clinicianId is required"))
                    .build();
        }

        String uri = "/v1/patients/" + patientId + "/orders/" + orderId + "/sign";

        // OperationService will JSON-serialize the request internally,
        // so here I pass a minimal stable representation:
        String requestJson = "{\"clinicianId\":\"" + request.getClinicianId() + "\"}";

        return operationService.execute(
                OperationType.SIGN_MEDICATION_ORDER,
                idempotencyKey,
                "POST",
                uri,
                requestJson,
                () -> signOrder(patientId, orderId, request)
        );
    }

    @Transactional
    protected MedicationOrderResponse signOrder(
            String patientId,
            String orderId,
            SignMedicationOrderRequest request
    ) {
        MedicationOrderEntity order = em.find(MedicationOrderEntity.class, orderId);

        if (order == null) {
            order = new MedicationOrderEntity();
            order.setOrderId(orderId);
            order.setPatientId(patientId);
            order.setStatus(MedicationOrderStatus.DRAFT);
            em.persist(order);
        }

        // If already signed, effect is idempotent (no changes)
        if (order.getStatus() != MedicationOrderStatus.SIGNED) {
            order.setStatus(MedicationOrderStatus.SIGNED);
            order.setSignedBy(request.getClinicianId());
            order.setSignedAt(Instant.now());
        }

        return new MedicationOrderResponse(
                order.getOrderId(),
                order.getPatientId(),
                order.getStatus(),
                order.getSignedBy(),
                order.getSignedAt()
        );
    }

    @POST
    @Path("/{orderId}/sign/undo")
    public Response undoSignOrder(
            @PathParam("patientId") String patientId,
            @PathParam("orderId") String orderId,
            @HeaderParam("Idempotency-Key") String idempotencyKey
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Idempotency-Key header is required"))
                    .build();
        }

        String uri = "/v1/patients/" + patientId + "/orders/" + orderId + "/sign/undo";
        String requestJson = "{}";

        return operationService.execute(
                OperationType.UNDO_SIGN_MEDICATION_ORDER,
                idempotencyKey,
                "POST",
                uri,
                requestJson,
                () -> undoSignOrder(patientId, orderId)
        );
    }

    @Transactional
    protected MedicationOrderResponse undoSignOrder(
            String patientId,
            String orderId
    ) {
        MedicationOrderEntity order = em.find(MedicationOrderEntity.class, orderId);

        if (order == null) {
            // Nothing existed -> create baseline draft order
            order = new MedicationOrderEntity();
            order.setOrderId(orderId);
            order.setPatientId(patientId);
            order.setStatus(MedicationOrderStatus.DRAFT);
            order.setSignedBy(null);
            order.setSignedAt(null);
            em.persist(order);
        } else if (order.getStatus() == MedicationOrderStatus.SIGNED) {
            // Undo sign
            order.setStatus(MedicationOrderStatus.DRAFT);
            order.setSignedBy(null);
            order.setSignedAt(null);
        }
        // If already DRAFT, no-op

        return new MedicationOrderResponse(
                order.getOrderId(),
                order.getPatientId(),
                order.getStatus(),
                order.getSignedBy(),
                order.getSignedAt()
        );
    }
}
