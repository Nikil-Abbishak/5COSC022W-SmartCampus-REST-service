package com.smartcampus.exception.mapper;

import com.smartcampus.exception.RoomNotEmptyException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * RoomNotEmptyExceptionMapper — Part 5.1 (409 Conflict)
 *
 * Scenario: A client attempts to DELETE a room that still has sensors assigned.
 *
 * Maps: RoomNotEmptyException → HTTP 409 Conflict
 *
 * Why 409 Conflict?
 *   HTTP 409 indicates that "the request could not be completed due to a conflict
 *   with the current state of the target resource." This is the precise semantic
 *   for our case: the room exists, the request is valid, but the server's current
 *   state (sensors still linked) prevents the operation.
 *
 * Response Body:
 *   A structured JSON error body is always returned — never a raw exception
 *   message or stack trace. This is the "leak-proof" API requirement.
 */
@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    private static final Logger LOGGER = Logger.getLogger(RoomNotEmptyExceptionMapper.class.getName());

    @Override
    public Response toResponse(RoomNotEmptyException ex) {
        LOGGER.warning("Room deletion blocked (409 Conflict): " + ex.getMessage());

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("status", 409);
        error.put("error", "Conflict");
        error.put("message", ex.getMessage());
        error.put("hint", "Use GET /api/v1/rooms/{roomId} to see which sensors are assigned, " +
                "then DELETE each sensor before retrying room deletion.");
        error.put("timestamp", System.currentTimeMillis());

        return Response
                .status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
