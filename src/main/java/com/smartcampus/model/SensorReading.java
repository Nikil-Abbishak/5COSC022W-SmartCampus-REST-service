package com.smartcampus.model;

/**
 * SensorReading.java — Core Resource Model
 *
 * Represents a single, immutable timestamped measurement event recorded by a Sensor.
 * SensorReadings form a historical log — once created, they are never modified (append-only).
 *
 * Each reading belongs to exactly one Sensor (accessed via the sub-resource path:
 * GET /api/v1/sensors/{sensorId}/readings).
 *
 * The id should be a UUID to guarantee global uniqueness across the entire campus
 * infrastructure, even in distributed deployments.
 */
public class SensorReading {

    /**
     * Unique reading event identifier.
     * Recommended: java.util.UUID.randomUUID().toString()
     * Example: "550e8400-e29b-41d4-a716-446655440000"
     */
    private String id;

    /**
     * The exact moment this measurement was captured, expressed as Unix epoch
     * time in milliseconds. Using epoch time (long) avoids timezone ambiguity
     * and simplifies time-series sorting.
     * Example: 1713600000000L = 2024-04-20T12:00:00Z
     */
    private long timestamp;

    /**
     * The actual metric value recorded by the hardware at the time of this reading.
     * The unit of measurement is implicit (defined by the Sensor's type):
     *   - Temperature sensor → degrees Celsius
     *   - CO2 sensor        → parts per million (ppm)
     *   - Occupancy sensor  → count of people
     */
    private double value;

    // =====================================================================
    // Constructors
    // =====================================================================

    /** No-arg constructor required by Jackson for JSON deserialization. */
    public SensorReading() {}

    public SensorReading(String id, long timestamp, double value) {
        this.id = id;
        this.timestamp = timestamp;
        this.value = value;
    }

    // =====================================================================
    // Getters & Setters
    // =====================================================================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "SensorReading{id='" + id + "', timestamp=" + timestamp + ", value=" + value + "}";
    }
}
