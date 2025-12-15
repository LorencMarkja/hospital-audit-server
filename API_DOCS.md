# Hospital Audit Server – API Documentation

Base URL (when running locally):

    http://localhost:8080/hospital/v1

All non-GET endpoints:

- Expect application/json bodies

- Require an Idempotency-Key header

If Idempotency-Key is missing, the server returns **400 Bad Request**.

## Conventions
### Headers

- `Content-Type: application/json`

- `Idempotency-Key: <string> (required for all state-changing endpoints)`

### Idempotency behavior

For the same Idempotency-Key:

If the same request (same method, URI, body, operation type) is retried:

- The server replays the stored response from the operation log.

If a different request is sent with the same key:

- The server returns **409 Conflict** (idempotency key reused inconsistently).

Each operation is also **domain-idempotent** (e.g., discharging an already discharged patient is a no-op).

##  Health
### GET /health

Simple liveness endpoint.

**Request**

    curl -i "http://localhost:8080/hospital/v1/health"


**Response 200**

    {
    "time": "2025-12-13T22:23:32.597614+01:00",
    "status": "UP"
    }

## Discharge Patient 
    POST /patients/{patientId}/discharge

Discharges a patient. Idempotent via `Idempotency-Key` and domain state.

Path params

- `patientId` - patient identifier

Headers

- `Idempotency-Key: <string> (required)`

- `Content-Type: application/json`

Request body

Currently no required fields, an empty object is accepted:

    {}


Example request

    curl -i -X POST \
    "http://localhost:8080/hospital/v1/patients/42/discharge" \
    -H "Idempotency-Key: discharge-1" \
    -H "Content-Type: application/json" \
    -d '{}'


**Response 200 - first call**

    {
    "patientId": "42",
    "discharged": true,
    "dischargedAt": "2025-12-13T23:07:02.641976Z"
    }


**Response 200 - retry with the same `Idempotency-Key`**

- Same JSON body as the first call (replayed from the operation log).

- No duplicate discharge.

Possible errors

- **400 Bad Request** - missing Idempotency-Key.

- **409 Conflict** - same Idempotency-Key reused for a different request.

- **500 Internal Server Error** - unexpected server error.

## Undo Discharge
    POST /patients/{patientId}/discharge/undo

Compensating action for discharge. Marks the patient as not discharged.

Path params

- `patientId` - patient identifier

Headers

- `Idempotency-Key: <string> (required)`

- `Content-Type: application/json`

Request body

    {}


Example request

    curl -i -X POST \
    "http://localhost:8080/hospital/v1/patients/42/discharge/undo" \
    -H "Idempotency-Key: discharge-undo-1" \
    -H "Content-Type: application/json" \
    -d '{}'


**Response 200**

    {
    "patientId": "42",
    "discharged": false,
    "dischargedAt": null
    }


Idempotency

- Repeating the call with the same Idempotency-Key returns the same JSON.

- If the patient is already not discharged, the operation is a no-op at domain level.

Possible errors

- **400 Bad Request** - missing Idempotency-Key.
- **409 Conflict** - key reused with a different undo request.
- **500 Internal Server Error**.

## Sign Medication Order
    POST /patients/{patientId}/orders/{orderId}/sign

Signs a medication order for a patient.

Path params

- `patientId` - patient identifier
- `orderId` - medication order identifier

Headers

- `Idempotency-Key: <string> (required)`
- `Content-Type: application/json`

Request body

    {
    "clinicianId": "dr.house"
    }


Example request

    curl -i -X POST \
    "http://localhost:8080/hospital/v1/patients/42/orders/ord-123/sign" \
    -H "Idempotency-Key: sign-1" \
    -H "Content-Type: application/json" \
    -d '{"clinicianId": "dr.house"}'


**Response 200**

    {
    "orderId": "ord-123",
    "patientId": "42",
    "status": "SIGNED",
    "signedBy": "dr.house",
    "signedAt": "2025-12-13T23:07:02.641976Z"
    }


Idempotency

Retrying with the same Idempotency-Key returns the same payload from the operation log.

If the order is already SIGNED, signing again is a domain-level no-op.

Possible errors

- **400 Bad Request**
Missing Idempotency-Key
clinicianId missing or empty in the JSON body**

- **409 Conflict**
Same Idempotency-Key reused with a different request (e.g. different body).

- **500 Internal Server Error**
Unexpected errors in business logic.


## Undo Sign Medication Order
    POST /patients/{patientId}/orders/{orderId}/sign/undo

Reverses a previously signed medication order.

Path params

- `patientId` - patient identifier
- `orderId` - medication order identifier

Headers

- `Idempotency-Key: <string> (required)`
- `Content-Type: application/json`

Request body

    {}


Example request

    curl -i -X POST \
    "http://localhost:8080/hospital/v1/patients/42/orders/ord-123/sign/undo" \
    -H "Idempotency-Key: sign-undo-1" \
    -H "Content-Type: application/json" \
    -d '{}'


**Response 200**

    {
    "orderId": "ord-123",
    "patientId": "42",
    "status": "DRAFT",
    "signedBy": null,
    "signedAt": null
    }


Idempotency

- Repeating with the same Idempotency-Key returns the same JSON.

- If the order is already DRAFT, undo is a domain-level no-op.

Possible errors

- **400 Bad Request** - missing Idempotency-Key
- **409 Conflict** - same key reused with a different undo request**
- **4500 Internal Server Error** - unexpected errors

## Quick End-to-End Test Flow (cURL)

You can use this sequence to exercise the whole flow manually.

    # Health
    curl -i "http://localhost:8080/hospital/v1/health"
    
    # Discharge patient 42
    curl -i -X POST \
    "http://localhost:8080/hospital/v1/patients/42/discharge" \
    -H "Idempotency-Key: discharge-1" \
    -H "Content-Type: application/json" \
    -d '{}'
    
    # Retry discharge with same key (idempotent)
    curl -i -X POST \
    "http://localhost:8080/hospital/v1/patients/42/discharge" \
    -H "Idempotency-Key: discharge-1" \
    -H "Content-Type: application/json" \
    -d '{}'
    
    # Sign medication order ord-123
    curl -i -X POST \
    "http://localhost:8080/hospital/v1/patients/42/orders/ord-123/sign" \
    -H "Idempotency-Key: sign-1" \
    -H "Content-Type: application/json" \
    -d '{"clinicianId": "dr.house"}'
    
    # Undo discharge
    curl -i -X POST \
    "http://localhost:8080/hospital/v1/patients/42/discharge/undo" \
    -H "Idempotency-Key: discharge-undo-1" \
    -H "Content-Type: application/json" \
    -d '{}'
    
    # Undo sign
    curl -i -X POST \
    "http://localhost:8080/hospital/v1/patients/42/orders/ord-123/sign/undo" \
    -H "Idempotency-Key: sign-undo-1" \
    -H "Content-Type: application/json" \
    -d '{}'
