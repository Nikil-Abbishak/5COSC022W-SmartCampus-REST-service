package com.smartcampus.exception.mapper;

import com.smartcampus.exception.LinkedResourceNotFoundException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * LinkedResourceNotFoundExceptionMapper — Part 5.2 (422 Unprocessable Entity)
 *
 * Scenario: A client attempts to POST a new Sensor with a roomId that does not
 * exist in the system — i.e., the JSON body is syntactically valid but contains
 * an unresolvable reference.
 *
 * Maps: LinkedResourceNotFoundException → HTTP 422 Unprocessable Entity
 *
 * Why 422 over 404?
 *   - HTTP 404 Not Found means the URL (resource) being requested does not exist.
 *     But the URL /api/v1/sensors IS valid — the sensors collection exists.
 *   - HTTP 422 Unprocessable Entity means: "The server understands the content type
 *     of the request entity, and the syntax is correct, but it was unable to process
 *     the contained instructions." This precisely describes our scenario: the JSON
 *     parses fine, but the roomId value inside it is semantically invalid.
 *   - Using 422 gives client developers actionable information: check the *values*
 *     in your request body, not the URL itself.
 *
 * Why not 400 Bad Request?
 *   400 typically indicates a syntactic problem (malformed JSON, wrong field types).
 *   Our request body is syntactically perfect — the problem is a missing reference,
 *   which is a semantic/business logic error, better expressed as 422.
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper
        implements ExceptionMapper<LinkedResourceNotFoundException> {

    private static final Logger LOGGER =
            Logger.getLogger(LinkedResourceNotFoundExceptionMapper.class.getName());

    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {
        LOGGER.warning("Dependency validation failed (422 Unprocessable Entity): " + ex.getMessage());

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("status", 422);
        error.put("error", "Unprocessable Entity");
        error.put("message", ex.getMessage());
        error.put("hint", "Verify the 'roomId' in your request body references an existing room. " +
                "Use GET /api/v1/rooms to see all valid rooms.");
        error.put("timestamp", System.currentTimeMillis());

        return Response
                .status(422)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
