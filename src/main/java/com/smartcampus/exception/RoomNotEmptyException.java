package com.smartcampus.exception;

/**
 * RoomNotEmptyException — Part 5: Error Handling
 * Thrown when attempting to delete a Room that still has active Sensors assigned.
 * Maps to HTTP 409 Conflict.
 * TO BE FULLY IMPLEMENTED IN PART 5.
 */
public class RoomNotEmptyException extends RuntimeException {
    public RoomNotEmptyException(String message) {
        super(message);
    }
}
