package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DiscoveryResource.java — API Discovery & HATEOAS Root Endpoint
 *
 * Mapped to: GET /api/v1
 *
 * ============================================================
 * PURPOSE (Addresses Report Question 1.2 — HATEOAS)
 * ============================================================
 *
 * This endpoint implements the HATEOAS (Hypermedia as the Engine of Application
 * State) principle from Roy Fielding's REST dissertation. Rather than requiring
 * clients to rely on external documentation to discover available resources,
 * the API itself provides navigational links within the response body.
 *
 * Benefits for client developers:
 *  1. Self-Discovery: A client hitting only this root URL immediately learns
 *     all available resource collections and their canonical URIs.
 *  2. Decoupling: Client code is not hard-coded with resource paths. If the
 *     server changes /rooms to /campus-rooms in a future version, clients
 *     following the link from this discovery endpoint adapt without code changes.
 *  3. Reduced Documentation Dependence: The API is partially self-documenting.
 *
 * @Path("/") — Handles requests to the @ApplicationPath root itself, i.e., GET /api/v1
 *              Using "/" is the correct way to map the root in Jersey 2.x.
 *              @Path("") can cause 500 errors due to ambiguous routing resolution.
 */
@Path("/")
public class DiscoveryResource {

    /** The semantic version of this API. */
    private static final String API_VERSION = "1.0.0";

    /** Base URI for constructing HATEOAS links. */
    private static final String BASE_URI = "http://localhost:8080/api/v1";

    /**
     * GET /api/v1 — Discovery Endpoint
     *
     * Returns a rich JSON object containing:
     *   - API name and version
     *   - Administrative contact information
     *   - A map of primary resource collections with their full URIs (HATEOAS links)
     *   - Brief description of each resource for self-documentation
     *
     * HTTP 200 OK is returned unconditionally — this endpoint has no failure modes.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response discover() {

        // Use LinkedHashMap to preserve insertion order in the JSON output,
        // making the response consistent and predictable for callers.
        Map<String, Object> response = new LinkedHashMap<>();

        // ── API Metadata ──────────────────────────────────────────────────
        response.put("service",     "Smart Campus Sensor & Room Management API");
        response.put("version",     API_VERSION);
        response.put("status",      "OPERATIONAL");
        response.put("description", "A JAX-RS RESTful API for managing campus rooms and IoT sensors.");

        // ── Administrative Contact ────────────────────────────────────────
        Map<String, String> contact = new LinkedHashMap<>();
        contact.put("team",  "Smart Campus Infrastructure Team");
        contact.put("email", "smartcampus-admin@university.ac.uk");
        contact.put("department", "Facilities Management & IT Services");
        response.put("contact", contact);

        // ── HATEOAS Resource Links ────────────────────────────────────────
        // This is the key HATEOAS element: a map of resource names to their
        // canonical URIs. Clients do not need external docs to find these paths.
        Map<String, Object> resources = new LinkedHashMap<>();

        // Rooms resource
        Map<String, String> roomsLink = new LinkedHashMap<>();
        roomsLink.put("href",        BASE_URI + "/rooms");
        roomsLink.put("description", "Manage campus rooms: list, create, retrieve, and decommission.");
        roomsLink.put("methods",     "GET, POST, GET/{id}, DELETE/{id}");
        resources.put("rooms", roomsLink);

        // Sensors resource
        Map<String, String> sensorsLink = new LinkedHashMap<>();
        sensorsLink.put("href",        BASE_URI + "/sensors");
        sensorsLink.put("description", "Manage IoT sensors: register, query by type, and retrieve details.");
        sensorsLink.put("methods",     "GET, GET?type={type}, POST, GET/{id}");
        resources.put("sensors", sensorsLink);

        // Sensor Readings sub-resource (nested)
        Map<String, String> readingsLink = new LinkedHashMap<>();
        readingsLink.put("href",        BASE_URI + "/sensors/{sensorId}/readings");
        readingsLink.put("description", "Access the historical reading log for a specific sensor.");
        readingsLink.put("methods",     "GET, POST");
        resources.put("sensorReadings", readingsLink);

        response.put("resources", resources);

        // ── API Documentation ─────────────────────────────────────────────
        response.put("documentation", BASE_URI + " (this endpoint)");
        response.put("timestamp", System.currentTimeMillis());

        return Response.ok(response).build();
    }
}
