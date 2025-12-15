# Hospital Audit Server

RESTful backend for critical hospital operations used by clinicians.

The goal is to support operations such as **“discharge patient”** and **“sign medication order”** with:

- **Idempotent operations** (safe client retries via `Idempotency-Key`)
- **Exactly-once effect per idempotency key** using a **persistent store**
- **Clear undo/compensation endpoints**
- **Robust error handling** for invalid requests and server failures

---

## Architecture

### Tech stack

- Java 17
- WildFly 38 bootable JAR (Jakarta EE 10)
- Jakarta EE APIs:
    - JAX-RS for REST endpoints
    - CDI for dependency injection
    - JPA for persistence
- H2 file database for persistence

### Structure

- `com.hospital.api`
    - `HospitalApplication` - JAX-RS bootstrap (`/v1`)
    - `HealthResource` - health check
    - `PatientResource` - `discharge` / `discharge/undo`
    - `MedicationOrderResource` - `sign` / `sign/undo`
- `com.hospital.api.dto`
    - Request/response DTOs (no JPA annotations)
- `com.hospital.persistence.entity`
    - `OperationEntity` - operation log (idempotency + audit)
    - `OperationStatus` - `RECEIVED` / `PROCESSING` / `COMPLETED` / `FAILED`
    - `OperationType` - `DISCHARGE_PATIENT` / `UNDO_DISCHARGE_PATIENT` / `SIGN_MEDICATION_ORDER` / `UNDO_SIGN_MEDICATION_ORDER`
    - `PatientEntity`, `MedicationOrderEntity` - domain data
- `com.hospital.service`
    - `OperationService` - generic idempotent operation wrapper
- `com.hospital.config`
    - `DataSourceConfig` - `@DataSourceDefinition` for `java:app/jdbc/HospitalDS`

### Persistence

- JPA persistence unit: `hospitalPU`
- Datasource: `java:app/jdbc/HospitalDS`
- H2 in **file mode**, so data survives restart:
    - This is sufficient to demonstrate “operations are not lost”
    - Can be swapped to MySQL/Postgres in a real deployment with no code changes

---

## Idempotency & Exactly-Once Effect

### Idempotency-Key pattern

Every **state-changing** request must include an `Idempotency-Key` header, for example:

`Idempotency-Key: <client-generated-uuid>`

All critical operations go through a single method in `OperationService` that receives:

- `OperationType`: what business operation is being performed (e.g. `DISCHARGE_PATIENT`)
- `Idempotency-Key`: client-provided unique key per logical operation
- HTTP method and URI
- The canonical request JSON as a string
- A `Supplier<T>` representing the business logic to execute

**Conceptual flow:**

1. Compute a hash of the request body (`bodyHash`).
2. Look up `OperationEntity` by `idempotencyKey`.
3. If an operation exists **and matches the same request** (method, URI, body hash, type):
    - Return the **stored response** (same HTTP status + JSON body).
    - Do **not** re-run the business logic supplier.
4. If no operation exists for that key:
    - Persist a new `OperationEntity` with `status = RECEIVED`.
    - Mark it as `PROCESSING` and run the business logic supplier.
    - Serialize the result to JSON, store it in `responseJson`, set `httpStatus` (e.g. `200`), and mark `status = COMPLETED`.
    - Return an HTTP response with that result.

The `operations` table has a **unique constraint on `idempotency_key`**, ensuring one row per key in the persistent store.  
Because everything runs in a **single JPA transaction** the operation log and domain state updates are committed atomically.

From the client’s point of view this gives **exactly one effect per idempotency key**, even with multiple retries.

### Domain-level idempotency

The domain logic itself is also idempotent:

- **Discharge**
    - If patient is already discharged -> no operation
- **Undo discharge**
    - If patient is already not discharged -> no operation
- **Sign medication order**
    - If already signed -> no operation
- **Undo sign**
    - If already draft -> no operation

Even if an operation accidentally runs twice, the domain state converges to the same final state.

---

## Undo / Compensation

The server exposes explicit compensating endpoints:

- `POST /patients/{id}/discharge/undo`  
  Reverses the effect of a discharge:
    - sets `discharged = false`
    - clears the discharge timestamp

- `POST /patients/{id}/orders/{orderId}/sign/undo`  
  Reverses signing a medication order:
    - sets status back to `DRAFT`
    - clears `signedBy` and `signedAt`

Each undo call:

- Has its own `OperationType` and operation log entry.
- Is idempotent (multiple undo calls leave the system in the same state).
- Provides traceability: you see the original operation and the undo in the `operations` table.

---

## Failure Handling & Guaranteed Delivery

### Invalid requests

- Missing `Idempotency-Key` → `400 Bad Request` + JSON error.
- Invalid payload for sign (e.g. missing `clinicianId`) → `400 Bad Request`.

### Server errors

- Exceptions in business logic cause the transaction to roll back.
- The operation can safely be retried with the same `Idempotency-Key`.

### Crashes / network failures

Because results are stored in a **persistent DB**:

- If the server crashes **after commit but before returning a response**,  
  a retry with the same `Idempotency-Key` returns the stored result.
- If it crashes **before commit**, there is no record, and a retry will execute the operation.

As long as the client retries with the same `Idempotency-Key`, operations have **exactly one durable effect** in the database.

---
## Setup & Run
### Prerequisites

- Java 17

- Maven 3.8+

### Build

From the project root, run: `mvn clean package`

This will compile the code, run tests, and build the following artifacts:

- target/hospital-audit-server-1.0.0-SNAPSHOT.war

- target/hospital-audit-server-bootable.jar

### Run

Start the bootable WildFly server with the application: `java -jar target/hospital-audit-server-bootable.jar`

The server will start on: `http://localhost:8080`

### Base API path

`http://localhost:8080/hospital/v1`

### Quick sanity check

Health endpoint: ` curl http://localhost:8080/hospital/v1/health`

Expected response (example)

`{
"time": "2025-12-13T22:23:32.597614+01:00",
"status": "UP"
}`


## Testing

Run unit tests with:

    mvn test

OperationServiceTest covers the core idempotent operation wrapper.

## Mobile Offline Integration (Design Only)

A mobile client can generate a UUID per logical operation and store it locally.
When offline, it queues requests (with body + Idempotency-Key).
Once connectivity is back, it replays them against the server in order.
Because the server enforces idempotency per Idempotency-Key and persists results,
retries are safe and each logical action has exactly one durable effect.

