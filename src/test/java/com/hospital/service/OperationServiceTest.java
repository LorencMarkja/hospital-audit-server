package com.hospital.service;

import com.hospital.persistence.entity.OperationEntity;
import com.hospital.persistence.entity.OperationStatus;
import com.hospital.persistence.entity.OperationType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OperationService.
 *
 * These tests run without WildFly or a real database.
 * EntityManager is mocked to focus on idempotency behavior.
 */
class OperationServiceTest {

    OperationService operationService;
    EntityManager em;
    TypedQuery<OperationEntity> query;

    @BeforeEach
    void setUp() {
        operationService = new OperationService();

        em = mock(EntityManager.class);
        query = mock(TypedQuery.class);

        operationService.setEntityManager(em);

        when(em.createQuery(anyString(), eq(OperationEntity.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @Test
    void execute_whenNoExistingOperation_persistsAndRunsBusinessLogic() {
        String idempotencyKey = "key-123";
        String method = "POST";
        String uri = "/v1/patients/42/discharge";
        String requestJson = "{}";

        // Simulate no existing operation for this key
        when(query.getSingleResult()).thenThrow(new NoResultException());

        AtomicInteger businessLogicCalls = new AtomicInteger();
        String businessResult = "OK";

        Response response = operationService.execute(
                OperationType.DISCHARGE_PATIENT,
                idempotencyKey,
                method,
                uri,
                requestJson,
                () -> {
                    businessLogicCalls.incrementAndGet();
                    return businessResult;
                }
        );

        // Business logic must run exactly once
        assertEquals(1, businessLogicCalls.get(), "Business logic should be called exactly once");

        // OperationEntity must be persisted with correct data
        ArgumentCaptor<OperationEntity> opCaptor = ArgumentCaptor.forClass(OperationEntity.class);
        verify(em).persist(opCaptor.capture());
        OperationEntity saved = opCaptor.getValue();

        assertEquals(idempotencyKey, saved.getIdempotencyKey());
        assertEquals(method, saved.getMethod());
        assertEquals(uri, saved.getUri());
        assertEquals(OperationType.DISCHARGE_PATIENT, saved.getType());
        assertEquals(OperationStatus.COMPLETED, saved.getStatus());
        assertEquals(200, saved.getHttpStatus());

        // HTTP response should be 200 and contain the business result
        assertEquals(200, response.getStatus());
        assertEquals(businessResult, response.getEntity());
    }

    @Test
    void execute_whenIdempotencyKeyReusedWithDifferentRequest_returnsConflictAndDoesNotRunLogic() {
        String idempotencyKey = "key-456";
        String method = "POST";
        String uri = "/v1/patients/42/discharge";

        String originalRequestJson = "{}";
        String differentRequestJson = "{\"foo\":\"bar\"}";

        // Build an existing operation representing an earlier call
        OperationEntity existing = new OperationEntity();
        existing.setIdempotencyKey(idempotencyKey);
        existing.setMethod(method);
        existing.setUri(uri);
        existing.setBodyHash("original-hash"); // deliberately different from new body hash
        existing.setHttpStatus(200);
        existing.setResponseJson("{\"dummy\":\"response\"}");
        existing.setType(OperationType.DISCHARGE_PATIENT);
        existing.setStatus(OperationStatus.COMPLETED);

        // Simulate a row already exists for this idempotency key
        when(query.getSingleResult()).thenReturn(existing);

        AtomicInteger businessLogicCalls = new AtomicInteger();

        Response response = operationService.execute(
                OperationType.DISCHARGE_PATIENT,
                idempotencyKey,
                method,
                uri,
                differentRequestJson, // DIFFERENT body
                () -> {
                    businessLogicCalls.incrementAndGet();
                    return "SHOULD_NOT_BE_CALLED";
                }
        );

    }
}
