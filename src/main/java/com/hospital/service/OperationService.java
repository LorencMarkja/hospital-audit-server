package com.hospital.service;

import com.hospital.persistence.entity.OperationEntity;
import com.hospital.persistence.entity.OperationStatus;
import com.hospital.persistence.entity.OperationType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@ApplicationScoped
public class OperationService {

    @PersistenceContext(unitName = "hospitalPU")
    private EntityManager em;

    private final Jsonb jsonb = JsonbBuilder.create();

    // --- only used from tests ---
    void setEntityManager(EntityManager em) {
        this.em = em;
    }

    @Transactional
    public <T> Response execute(
            OperationType type,
            String idempotencyKey,
            String method,
            String uri,
            String requestJson,
            Supplier<T> businessLogic
    ) {
        String bodyHash = hashBody(requestJson == null ? "" : requestJson);

        // Check if operation already exists
        OperationEntity existing = findByIdempotencyKey(idempotencyKey);

        if (existing != null) {
            // Check for semantic mismatch
            if (!Objects.equals(existing.getMethod(), method)
                    || !Objects.equals(existing.getUri(), uri)
                    || !Objects.equals(existing.getBodyHash(), bodyHash)) {

                return Response.status(Response.Status.CONFLICT)
                        .entity("{\"error\":\"Idempotency key reused with different request\"}")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }

            // Return stored response
            return Response.status(existing.getHttpStatus())
                    .entity(existing.getResponseJson())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        // If not found I need to create a new operation row
        OperationEntity op = new OperationEntity();
        op.setIdempotencyKey(idempotencyKey);
        op.setMethod(method);
        op.setUri(uri);
        op.setBodyHash(bodyHash);
        op.setType(type);
        op.setStatus(OperationStatus.RECEIVED);
        op.setRequestJson(requestJson);

        em.persist(op);
        em.flush(); // ensures id + constraints are applied

        try {
            op.setStatus(OperationStatus.PROCESSING);
            T result = businessLogic.get();

            String responseJson = jsonb.toJson(result);
            op.setResponseJson(responseJson);
            op.setHttpStatus(Response.Status.OK.getStatusCode());
            op.setStatus(OperationStatus.COMPLETED);

            // JAX-RS will serialize result again; stored JSON is for retries
            return Response.ok(result).build();
        } catch (RuntimeException e) {
            op.setStatus(OperationStatus.FAILED);
            op.setHttpStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            op.setResponseJson("{\"error\":\"Internal error processing operation\"}");
            throw e;
        }
    }

    private OperationEntity findByIdempotencyKey(String key) {
        TypedQuery<OperationEntity> q = em.createQuery(
                "SELECT o FROM OperationEntity o WHERE o.idempotencyKey = :key",
                OperationEntity.class
        );
        q.setParameter("key", key);
        List<OperationEntity> list = q.getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    private String hashBody(String body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(body.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
