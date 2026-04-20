package com.smartcampus.exception;

/**
 * SensorUnavailableException — Part 5: Error Handling
 * Thrown when a reading is POSTed to a sensor in MAINTENANCE or OFFLINE status.
 * Maps to HTTP 403 Forbidden.
 * TO BE FULLY IMPLEMENTED IN PART 5.
 */
public class SensorUnavailableException extends RuntimeException {
    public SensorUnavailableException(String message) {
        super(message);
    }
}
