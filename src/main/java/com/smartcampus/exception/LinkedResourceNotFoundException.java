package com.smartcampus.exception;

/**
 * LinkedResourceNotFoundException — Part 5: Error Handling
 * Thrown when a sensor references a roomId that does not exist in the DataStore.
 * Maps to HTTP 422 Unprocessable Entity.
 * TO BE FULLY IMPLEMENTED IN PART 5.
 */
public class LinkedResourceNotFoundException extends RuntimeException {
    public LinkedResourceNotFoundException(String message) {
        super(message);
    }
}
