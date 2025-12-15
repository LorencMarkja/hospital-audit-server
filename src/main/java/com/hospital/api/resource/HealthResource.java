package com.hospital.api.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.OffsetDateTime;
import java.util.Map;

@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

    // simple endpoint used for readiness tests
    @GET
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "time", OffsetDateTime.now().toString()
        );
    }
}
