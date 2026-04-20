package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SensorReadingResource.java — Sensor Readings Sub-Resource (Part 4)
 *
 * This class is NEVER registered directly with JAX-RS. Instead, it is
 * instantiated and returned by SensorResource's sub-resource locator method:
 *
 *   @Path("/{sensorId}/readings")
 *   public SensorReadingResource getReadingsLocator(...) {
 *       return new SensorReadingResource(sensorId);
 *   }
 *
 * Jersey then inspects this returned object for @GET/@POST method annotations
 * and routes the request accordingly.
 *
 * Effective URL mapping:
 *   GET  /api/v1/sensors/{sensorId}/readings    → getAllReadings()
 *   POST /api/v1/sensors/{sensorId}/readings    → addReading()
 *
 * This class does NOT carry a @Path annotation — its path is defined by
 * the locator method in SensorResource.
 *
 * ARCHITECTURAL BENEFIT (Part 4.1 Report Answer):
 *   By delegating to this separate class, SensorResource remains focused and
 *   bounded. SensorReadingResource can grow (e.g., add GET by reading ID,
 *   DELETE, or statistics endpoints) without affecting the parent resource.
 *   This mirrors the microservice philosophy applied at the class level.
 */
@Produces(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    /** The specific sensor ID this resource is scoped to. Set by the locator. */
    private final String sensorId;

    /**
     * Constructor called by the sub-resource locator in SensorResource.
     * The sensorId is "injected" here, scoping all operations to this sensor.
     *
     * @param sensorId  The sensor whose readings this resource manages.
     */
    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // =========================================================================
    // GET /api/v1/sensors/{sensorId}/readings — Retrieve all readings history
    // =========================================================================

    /**
     * Returns the complete historical log of readings for this sensor.
     * Readings are stored in insertion order (chronological).
     *
     * @return HTTP 200 OK with JSON array of SensorReading objects,
     *         HTTP 404 if the sensor does not exist.
     */
    @GET
    public Response getAllReadings() {
        // Verify the sensor exists
        Sensor sensor = DataStore.getSensors().get(sensorId);
        if (sensor == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", 404);
            error.put("error", "Not Found");
            error.put("message", "Sensor with ID '" + sensorId + "' does not exist.");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }

        List<SensorReading> readings = DataStore.getOrCreateReadings(sensorId);
        return Response.ok(readings).build();
    }

    // =========================================================================
    // POST /api/v1/sensors/{sensorId}/readings — Record a new measurement
    // =========================================================================

    /**
     * Appends a new sensor reading to the historical log for this sensor.
     *
     * State Constraint — Part 5.3 (403 Forbidden):
     *   If the sensor's status is "MAINTENANCE" or "OFFLINE", it cannot accept
     *   new readings (the physical device is disconnected). A SensorUnavailableException
     *   is thrown, which maps to HTTP 403 Forbidden.
     *
     * Side Effect — Part 4.2:
     *   A successful POST MUST update the parent Sensor's currentValue field to
     *   the value in this new reading. This keeps the Sensor summary object
     *   consistent with the latest reading event.
     *
     * Auto-populated fields:
     *   - id        → UUID generated server-side (guarantees global uniqueness)
     *   - timestamp → System.currentTimeMillis() (server-authoritative)
     *
     * @param reading  A partial SensorReading from the client (only value is required).
     * @return HTTP 201 Created with Location header and the full SensorReading body.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addReading(SensorReading reading) {
        // 1. Verify the sensor exists
        Sensor sensor = DataStore.getSensors().get(sensorId);
        if (sensor == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", 404);
            error.put("error", "Not Found");
            error.put("message", "Sensor with ID '" + sensorId + "' does not exist.");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }

        // 2. State constraint: reject readings for MAINTENANCE or OFFLINE sensors
        String status = sensor.getStatus();
        if ("MAINTENANCE".equalsIgnoreCase(status) || "OFFLINE".equalsIgnoreCase(status)) {
            // Throw custom exception → mapped to HTTP 403 by SensorUnavailableExceptionMapper
            throw new SensorUnavailableException(
                "Sensor '" + sensorId + "' is currently in '" + status + "' state and cannot " +
                "accept new readings. The device must be in ACTIVE status to record data."
            );
        }

        // 3. Server-authoritative fields: always override client-supplied id/timestamp
        reading.setId(UUID.randomUUID().toString());
        reading.setTimestamp(System.currentTimeMillis());

        // 4. Persist the reading (synchronised to prevent concurrent list corruption)
        List<SensorReading> readings = DataStore.getOrCreateReadings(sensorId);
        synchronized (readings) {
            readings.add(reading);
        }

        // 5. SIDE EFFECT (Part 4.2 requirement): update parent sensor's currentValue
        sensor.setCurrentValue(reading.getValue());

        // 6. Build Location URI for the new reading resource
        URI location = UriBuilder
                .fromUri("http://localhost:8080/api/v1/sensors/{sensorId}/readings/{readingId}")
                .build(sensorId, reading.getId());

        return Response.created(location).entity(reading).build();
    }
}
