package com.smartcampus.model;

/**
 * Sensor.java — Core Resource Model
 *
 * Represents a physical sensor device deployed within a Room on campus.
 * Sensors are the primary data producers in the Smart Campus system —
 * they continuously record environmental and operational metrics.
 *
 * Valid status values (enforced by business logic, not enum, for flexibility):
 *   - "ACTIVE"      : Sensor is operational and accepting new readings.
 *   - "MAINTENANCE" : Sensor is under service; new readings are FORBIDDEN (HTTP 403).
 *   - "OFFLINE"     : Sensor is disconnected; treated similarly to MAINTENANCE.
 *
 * The roomId field acts as a "foreign key" linking the sensor to its parent Room.
 * Referential integrity is enforced at the API layer: you cannot register a sensor
 * with a roomId that does not exist in the DataStore.
 */
public class Sensor {

    /** Unique identifier for this sensor (e.g., "TEMP-001", "CO2-042"). */
    private String id;

    /**
     * Sensor category, used for filtered queries.
     * Examples: "Temperature", "CO2", "Occupancy", "Humidity".
     */
    private String type;

    /**
     * Current operational state of the sensor.
     * Accepted values: "ACTIVE", "MAINTENANCE", "OFFLINE".
     */
    private String status;

    /**
     * The most recent measurement recorded by this sensor.
     * This field is updated as a side effect whenever a new SensorReading is POSTed
     * to /api/v1/sensors/{sensorId}/readings.
     */
    private double currentValue;

    /**
     * The ID of the Room where this sensor is physically installed.
     * Must reference a valid Room.id in the DataStore.
     */
    private String roomId;

    // =====================================================================
    // Constructors
    // =====================================================================

    /** No-arg constructor required by Jackson for JSON deserialization. */
    public Sensor() {}

    public Sensor(String id, String type, String status, double currentValue, String roomId) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.currentValue = currentValue;
        this.roomId = roomId;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    @Override
    public String toString() {
        return "Sensor{id='" + id + "', type='" + type + "', status='" + status
                + "', currentValue=" + currentValue + ", roomId='" + roomId + "'}";
    }
}
