package com.smartcampus.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Room.java — Core Resource Model
 *
 * Represents a physical space on campus (e.g., a lecture hall, lab, or library).
 * Each Room acts as a container for Sensors. The sensorIds list maintains referential
 * integrity: a Room "owns" all sensors whose Sensor.roomId matches this Room's id.
 *
 * This POJO is intentionally kept as a plain Java object (no JAX-RS or framework
 * annotations) to preserve clean architectural separation between the data model
 * and the transport layer.
 */
public class Room {

    /** Unique identifier for this room (e.g., "LIB-301", "LAB-101"). */
    private String id;

    /** Human-readable name for display in UIs (e.g., "Library Quiet Study"). */
    private String name;

    /** Maximum occupancy as mandated by fire-safety regulations. */
    private int capacity;

    /**
     * List of sensor IDs currently deployed in this room.
     * Initialized to an empty list to prevent NullPointerExceptions
     * when adding the first sensor to a new room.
     */
    private List<String> sensorIds = new ArrayList<>();

    // =====================================================================
    // Constructors
    // =====================================================================

    /** No-arg constructor required by Jackson for JSON deserialization. */
    public Room() {}

    public Room(String id, String name, int capacity) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public List<String> getSensorIds() {
        return sensorIds;
    }

    public void setSensorIds(List<String> sensorIds) {
        this.sensorIds = sensorIds;
    }

    @Override
    public String toString() {
        return "Room{id='" + id + "', name='" + name + "', capacity=" + capacity
                + ", sensorIds=" + sensorIds + "}";
    }
}
