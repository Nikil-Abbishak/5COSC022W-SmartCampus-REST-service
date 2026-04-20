package com.smartcampus;

import com.smartcampus.filter.LoggingFilter;
import com.smartcampus.exception.mapper.GlobalExceptionMapper;
import com.smartcampus.exception.mapper.LinkedResourceNotFoundExceptionMapper;
import com.smartcampus.exception.mapper.RoomNotEmptyExceptionMapper;
import com.smartcampus.exception.mapper.SensorUnavailableExceptionMapper;
import com.smartcampus.resource.DiscoveryResource;
import com.smartcampus.resource.RoomResource;
import com.smartcampus.resource.SensorResource;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * SmartCampusApplication.java — JAX-RS Application Entry Point
 *
 * This class is the cornerstone of the JAX-RS deployment model. By
 * extending javax.ws.rs.core.Application and annotating it with
 * @ApplicationPath, we establish a versioned, contract-based entry
 * point for the entire API without any web.xml configuration.
 *
 * @ApplicationPath("/api/v1"):
 *   - Sets the root URL segment for all resources in this application.
 *   - Combined with the embedded server's BASE_URI (http://localhost:8080/),
 *     all endpoints are prefix-rooted at: http://localhost:8080/api/v1/
 *   - This explicit versioning strategy allows future API versions (v2, v3)
 *     to coexist without breaking existing clients.
 *
 * Design Note — Manual Registration vs. Auto-Scanning:
 *   We override getClasses() to EXPLICITLY register every resource,
 *   provider, and feature. This is the professional approach because:
 *   1. It makes the application's surface area immediately visible.
 *   2. It avoids classpath-scanning overhead in large applications.
 *   3. It prevents accidental registration of test or internal classes.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> classes = new HashSet<>();

        // ===== Resource Classes (JAX-RS endpoints) =====
        classes.add(DiscoveryResource.class);   // GET /api/v1
        classes.add(RoomResource.class);         // /api/v1/rooms
        classes.add(SensorResource.class);       // /api/v1/sensors

        // ===== Exception Mappers (Part 5: Error Handling) =====
        // These transform custom exceptions into structured JSON HTTP responses.
        classes.add(RoomNotEmptyExceptionMapper.class);
        classes.add(LinkedResourceNotFoundExceptionMapper.class);
        classes.add(SensorUnavailableExceptionMapper.class);
        classes.add(GlobalExceptionMapper.class);           // Catch-all safety net

        // ===== Filters (Part 5: Observability) =====
        classes.add(LoggingFilter.class);

        // ===== Jersey Feature: Jackson JSON Provider =====
        // Enables automatic serialization/deserialization of POJOs to/from JSON.
        // Without this, JAX-RS cannot parse JSON request bodies or produce JSON responses.
        classes.add(JacksonFeature.class);

        return classes;
    }
}
