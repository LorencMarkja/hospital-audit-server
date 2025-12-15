package com.hospital.api;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS application bootstrap.
 * All REST resources will be under /v1.
 */
@ApplicationPath("/v1")
public class HospitalApplication extends Application {
    // empty on purpose
}
