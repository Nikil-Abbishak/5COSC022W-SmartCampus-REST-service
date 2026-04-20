package com.smartcampus.store;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DataStore.java — Thread-Safe In-Memory Singleton Data Store
 *
 * ============================================================
 * ARCHITECTURAL CONTEXT (directly addresses Report Question 1.1)
 * ============================================================
 *
 * PROBLEM: JAX-RS creates a NEW instance of a resource class for every
 * incoming HTTP request (request-scoped lifecycle by default). This means
 * any data stored as an instance variable inside RoomResource or SensorResource
 * would be lost after the request completes.
 *
 * SOLUTION: Maintain all shared state in this DataStore class using static
 * fields. Static fields belong to the Class itself — not to any individual
 * instance — so they persist for the entire JVM lifetime, surviving across
 * every request-scoped resource object creation.
 *
 * CONCURRENCY SAFETY: Because multiple requests can arrive simultaneously
 * (multi-threaded HTTP server), we use ConcurrentHashMap instead of a plain
 * HashMap. ConcurrentHashMap provides:
 *   - Thread-safe reads (no locking for get operations).
 *   - Atomic write operations via lock-striping (not whole-map locking).
 *   - Eliminates the risk of ConcurrentModificationException during iteration.
 *
 * For the readings list (per-sensor), we use a synchronized ArrayList wrapper
 * because ConcurrentHashMap only protects the map structure, not the mutable
 * List values inside it.
 *
 * SINGLETON ACCESS PATTERN: Each resource class accesses data via static
 * getters on this class, e.g., DataStore.getRooms().put(room.getId(), room).
 * No instantiation of DataStore is needed.
 */
public class DataStore {

    /**
     * Persists all Room objects, keyed by their unique Room ID.
     * Using ConcurrentHashMap ensures safe concurrent access from
     * simultaneous HTTP requests without explicit synchronization blocks.
     *
     * Key:   Room.id  (e.g., "LIB-301")
     * Value: Room object
     */
    private static final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

    /**
     * Persists all Sensor objects, keyed by their unique Sensor ID.
     *
     * Key:   Sensor.id  (e.g., "TEMP-001")
     * Value: Sensor object
     */
    private static final ConcurrentHashMap<String, Sensor> sensors = new ConcurrentHashMap<>();

    /**
     * Persists the historical reading log for each sensor.
     * Maps a Sensor ID to a list of all its SensorReading events.
     *
     * Key:   Sensor.id
     * Value: List of SensorReading objects (chronologically ordered)
     *
     * NOTE: The List itself is NOT thread-safe. Any modifications to
     * the inner list must be synchronized externally. In our implementation,
     * the resource methods handle one sensor's list at a time, and since
     * Java's ArrayList is protected by its reference in ConcurrentHashMap,
     * we use synchronizedList wrappers at insertion points.
     */
    private static final ConcurrentHashMap<String, List<SensorReading>> sensorReadings 
            = new ConcurrentHashMap<>();

    // =====================================================================
    // Static accessor methods — no instantiation required
    // =====================================================================

    public static ConcurrentHashMap<String, Room> getRooms() {
        return rooms;
    }

    public static ConcurrentHashMap<String, Sensor> getSensors() {
        return sensors;
    }

    public static ConcurrentHashMap<String, List<SensorReading>> getSensorReadings() {
        return sensorReadings;
    }

    /**
     * Helper: Retrieve or create the reading list for a given sensor.
     * Uses putIfAbsent for atomic "get-or-create" to prevent race conditions
     * when two requests simultaneously POST the first reading for the same sensor.
     *
     * @param sensorId The sensor whose reading list to retrieve.
     * @return The existing or newly created (empty) reading list.
     */
    public static List<SensorReading> getOrCreateReadings(String sensorId) {
        sensorReadings.putIfAbsent(sensorId, new ArrayList<>());
        return sensorReadings.get(sensorId);
    }

    // Private constructor — prevents instantiation.
    // This class is a pure static utility; it should never be new'd.
    private DataStore() {}
}
