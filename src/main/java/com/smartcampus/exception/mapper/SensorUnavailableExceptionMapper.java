package com.smartcampus.exception.mapper;

import com.smartcampus.exception.SensorUnavailableException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * SensorUnavailableExceptionMapper — Part 5.3 (403 Forbidden)
 *
 * Scenario: A client attempts to POST a new reading to a sensor that is currently
 * in "MAINTENANCE" or "OFFLINE" state. The physical device is disconnected and
 * cannot produce valid measurements.
 *
 * Maps: SensorUnavailableException → HTTP 403 Forbidden
 *
 * Why 403 Forbidden?
 *   HTTP 403 means "The server understood the request but refuses to fulfill it."
 *   This is appropriate here because:
 *   - The sensor resource EXISTS (it would be 404 if it didn't).
 *   - The request is well-formed (it would be 400 if the JSON was invalid).
 *   - BUT the server refuses to process it due to the sensor's current STATE.
 *   - The client knows what they're doing, but the server's business rules
 *     prohibit the operation in the current state.
 *
 *   This is distinct from 401 Unauthorized (authentication/identity issue).
 *   403 indicates that even a correctly authenticated client is forbidden from
 *   performing this specific operation in the current application state.
 *
 * Response Body:
 *   Always returns structured JSON — never a stack trace or plain text.
 */
@Provider
public class SensorUnavailableExceptionMapper
        implements ExceptionMapper<SensorUnavailableException> {

    private static final Logger LOGGER =
            Logger.getLogger(SensorUnavailableExceptionMapper.class.getName());

    @Override
    public Response toResponse(SensorUnavailableException ex) {
        LOGGER.warning("Reading rejected - sensor unavailable (403 Forbidden): " + ex.getMessage());

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("status", 403);
        error.put("error", "Forbidden");
        error.put("message", ex.getMessage());
        error.put("hint", "Update the sensor's status to 'ACTIVE' using a PUT request before " +
                "attempting to record new readings.");
        error.put("timestamp", System.currentTimeMillis());

        return Response
                .status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
