package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;
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
import java.util.stream.Collectors;

/**
 * SensorResource.java — Sensor Operations (Parts 3 & 4)
 *
 * Manages the /api/v1/sensors resource collection.
 *
 * Endpoints:
 *   GET    /api/v1/sensors              — List all sensors (supports ?type= filter)
 *   POST   /api/v1/sensors              — Register a new sensor (validates roomId)
 *   GET    /api/v1/sensors/{sensorId}   — Fetch a specific sensor
 *   DELETE /api/v1/sensors/{sensorId}   — Remove a sensor
 *
 * Sub-Resource Locator (Part 4):
 *   ANY    /api/v1/sensors/{sensorId}/readings — Delegates to SensorReadingResource
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {

    // =========================================================================
    // GET /api/v1/sensors — List sensors, with optional type filter
    // =========================================================================

    /**
     * Returns all registered sensors. Supports optional filtering by sensor type.
     *
     * Query Parameter: ?type={type}
     *   If provided, only sensors whose type matches the parameter are returned.
     *   The match is case-insensitive for better usability.
     *   Example: GET /api/v1/sensors?type=CO2
     *   If omitted, all sensors are returned.
     *
     * This implements Part 3.2 — Filtered Retrieval.
     *
     * @param type  Optional query parameter to filter by sensor type.
     * @return HTTP 200 OK with (filtered) JSON array of sensors.
     */
    @GET
    public Response getSensors(@QueryParam("type") String type) {
        List<Sensor> result;

        if (type != null && !type.trim().isEmpty()) {
            // Filter: case-insensitive type match for maximum usability
            result = DataStore.getSensors().values().stream()
                    .filter(s -> s.getType() != null && s.getType().equalsIgnoreCase(type.trim()))
                    .collect(Collectors.toList());
        } else {
            // No filter — return all sensors
            result = DataStore.getSensors().values().stream()
                    .collect(Collectors.toList());
        }

        return Response.ok(result).build();
    }

    // =========================================================================
    // POST /api/v1/sensors — Register a new sensor
    // =========================================================================

    /**
     * Registers a new sensor in the system.
     *
     * Integrity Validation (Part 3.1):
     *   Before persisting the sensor, this method verifies that the roomId
     *   specified in the request body references an existing Room in the DataStore.
     *   If the room does not exist, a LinkedResourceNotFoundException is thrown,
     *   which is mapped to HTTP 422 Unprocessable Entity.
     *
     * ID Auto-Generation:
     *   If the client does not supply a sensor ID, a UUID is auto-generated.
     *   This mirrors real-world IoT device provisioning systems.
     *
     * Room Link Update:
     *   On successful registration, the sensor's ID is added to the parent Room's
     *   sensorIds list to maintain bidirectional consistency.
     *
     * @param sensor  The Sensor object deserialized from the JSON request body.
     * @return HTTP 201 Created with Location header and Sensor body.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor) {
        // 1. Validate roomId is provided
        if (sensor.getRoomId() == null || sensor.getRoomId().trim().isEmpty()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", 400);
            error.put("error", "Bad Request");
            error.put("message", "Sensor 'roomId' field is required.");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        // 2. Referential integrity check — roomId must reference an existing room
        if (!DataStore.getRooms().containsKey(sensor.getRoomId())) {
            // Throw custom exception → mapped to HTTP 422 by LinkedResourceNotFoundExceptionMapper
            throw new LinkedResourceNotFoundException(
                "Cannot register sensor: roomId '" + sensor.getRoomId() + "' does not reference " +
                "an existing room. Please create the room first before assigning sensors to it."
            );
        }

        // 3. Auto-generate sensor ID if not provided
        if (sensor.getId() == null || sensor.getId().trim().isEmpty()) {
            sensor.setId(UUID.randomUUID().toString());
        }

        // 4. Check for duplicate sensor ID
        if (DataStore.getSensors().containsKey(sensor.getId())) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", 409);
            error.put("error", "Conflict");
            error.put("message", "A sensor with ID '" + sensor.getId() + "' already exists.");
            return Response.status(Response.Status.CONFLICT).entity(error).build();
        }

        // 5. Default status to ACTIVE if not provided
        if (sensor.getStatus() == null || sensor.getStatus().trim().isEmpty()) {
            sensor.setStatus("ACTIVE");
        }

        // 6. Persist the sensor
        DataStore.getSensors().put(sensor.getId(), sensor);

        // 7. Update the parent Room's sensorIds list (bidirectional consistency)
        DataStore.getRooms().get(sensor.getRoomId()).getSensorIds().add(sensor.getId());

        // 8. Initialise an empty reading history for this sensor
        DataStore.getOrCreateReadings(sensor.getId());

        // 9. Build Location URI
        URI location = UriBuilder.fromUri("http://localhost:8080/api/v1/sensors/{id}")
                .build(sensor.getId());

        return Response.created(location).entity(sensor).build();
    }

    // =========================================================================
    // GET /api/v1/sensors/{sensorId} — Get a specific sensor
    // =========================================================================

    /**
     * Fetches detailed information for a single sensor.
     *
     * @param sensorId  Sensor ID from the URL path.
     * @return HTTP 200 OK with Sensor object, or HTTP 404 if not found.
     */
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = DataStore.getSensors().get(sensorId);
        if (sensor == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", 404);
            error.put("error", "Not Found");
            error.put("message", "Sensor with ID '" + sensorId + "' does not exist.");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }
        return Response.ok(sensor).build();
    }

    // =========================================================================
    // DELETE /api/v1/sensors/{sensorId} — Remove a sensor
    // =========================================================================

    /**
     * Removes a sensor from the system.
     * Also cleans up the sensor ID from the parent room's sensorIds list,
     * and removes the associated readings history.
     *
     * @param sensorId  Sensor ID from the URL path.
     * @return HTTP 204 No Content on success, HTTP 404 if not found.
     */
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = DataStore.getSensors().get(sensorId);
        if (sensor == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", 404);
            error.put("error", "Not Found");
            error.put("message", "Sensor with ID '" + sensorId + "' does not exist.");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }

        // Remove sensor ID from parent room's list
        if (sensor.getRoomId() != null && DataStore.getRooms().containsKey(sensor.getRoomId())) {
            DataStore.getRooms().get(sensor.getRoomId()).getSensorIds().remove(sensorId);
        }

        // Remove sensor and its readings history
        DataStore.getSensors().remove(sensorId);
        DataStore.getSensorReadings().remove(sensorId);

        return Response.noContent().build();
    }

    // =========================================================================
    // Sub-Resource Locator (Part 4) — Delegate readings to SensorReadingResource
    // =========================================================================

    /**
     * Sub-Resource Locator for sensor readings.
     *
     * This method is the cornerstone of Part 4. It implements the JAX-RS
     * Sub-Resource Locator pattern: instead of handling the nested requests
     * itself, this method returns an instance of SensorReadingResource, passing
     * the sensorId as context. Jersey then routes any further path segments
     * (GET /, POST /) to that returned object.
     *
     * Why @Path("{sensorId}/readings") here with NO HTTP method annotation:
     *   JAX-RS distinguishes between "sub-resource methods" (have @GET/@POST etc.)
     *   and "sub-resource locators" (have only @Path). The absence of @GET/@POST
     *   signals to Jersey that this is a locator — it should delegate routing
     *   to the returned object's @GET/@POST methods.
     *
     * Benefits of this pattern (Part 4.1 answer):
     *   - Keeps SensorResource focused on sensor identity/management.
     *   - SensorReadingResource handles all reading history logic independently.
     *   - Each class is independently testable and bounded in size.
     *
     * @param sensorId  The sensor whose readings are being accessed.
     * @return A configured SensorReadingResource for Jersey to continue routing.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsLocator(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}
