# Smart Campus Sensor & Room Management API

**Module:** 5COSC022W — Client-Server Architectures  
**Technology:** Pure JAX-RS (Jersey 2.39.1) + Grizzly Embedded HTTP Server  
**Persistence:** In-memory (`ConcurrentHashMap`) — No SQL, No Database  
**API Root:** `http://localhost:8080/api/v1`

---

## Table of Contents

1. [Overview](#overview)
2. [Project Structure](#project-structure)
3. [How to Build and Run](#how-to-build-and-run)
4. [API Endpoints Reference](#api-endpoints-reference)
5. [Sample curl Commands](#sample-curl-commands)
6. [Conceptual Report — Question Answers](#conceptual-report--question-answers)

---

## Overview

This project implements a fully RESTful API for the university's "Smart Campus" initiative. The API manages:

- **Rooms** — Physical campus spaces (lecture halls, labs, libraries)
- **Sensors** — IoT devices (Temperature, CO2, Occupancy) deployed within rooms
- **Sensor Readings** — Historical, timestamped measurement logs per sensor

The service demonstrates industry-standard REST practices including versioned entry points, resource hierarchy, sub-resource locators, structured error handling with custom ExceptionMappers, and HATEOAS-inspired API discovery.

---

## Project Structure

```
SmartCampusAPI/
├── pom.xml                          ← Maven build (Jersey + Grizzly + Jackson)
└── src/main/java/com/smartcampus/
    ├── Main.java                    ← Embedded Grizzly server launcher
    ├── SmartCampusApplication.java  ← @ApplicationPath("/api/v1") JAX-RS bootstrap
    ├── model/
    │   ├── Room.java                ← POJO: id, name, capacity, sensorIds
    │   ├── Sensor.java              ← POJO: id, type, status, currentValue, roomId
    │   └── SensorReading.java       ← POJO: id, timestamp, value
    ├── store/
    │   └── DataStore.java           ← Thread-safe ConcurrentHashMap singleton
    ├── resource/
    │   ├── DiscoveryResource.java   ← GET /api/v1/  (HATEOAS root)
    │   ├── RoomResource.java        ← /api/v1/rooms (Part 2)
    │   ├── SensorResource.java      ← /api/v1/sensors (Parts 3 & 4)
    │   └── SensorReadingResource.java ← Sub-resource locator target (Part 4)
    ├── exception/
    │   ├── RoomNotEmptyException.java
    │   ├── LinkedResourceNotFoundException.java
    │   ├── SensorUnavailableException.java
    │   └── mapper/
    │       ├── RoomNotEmptyExceptionMapper.java        ← 409 Conflict
    │       ├── LinkedResourceNotFoundExceptionMapper.java ← 422 Unprocessable Entity
    │       ├── SensorUnavailableExceptionMapper.java   ← 403 Forbidden
    │       └── GlobalExceptionMapper.java              ← 500 Safety Net
    └── filter/
        └── LoggingFilter.java       ← Request/Response observability (Part 5)
```

---

## How to Build and Run

### Prerequisites

- **Java 11+** (tested with Java 24.0.1)
- **Apache Maven 3.6+** (tested with Maven 3.9.15)

### Step 1 — Clone the Repository

```bash
git clone https://github.com/<your-username>/5COSC022W-SmartCampus-REST-service.git
cd 5COSC022W-SmartCampus-REST-service
```

### Step 2 — Build the Executable JAR

```bash
mvn clean package
```

This produces `target/SmartCampusAPI-1.0.0.jar` — a self-contained executable with all dependencies (Jersey, Grizzly, Jackson) bundled inside via the Maven Shade plugin.

### Step 3 — Start the Server

```bash
java -jar target/SmartCampusAPI-1.0.0.jar
```

The API will be available at **`http://localhost:8080/api/v1`**.  
Press `Ctrl+C` to stop the server gracefully.

**Expected startup output:**
```
INFO: Started listener bound to [localhost:8080]
INFO: Smart Campus API started successfully.
INFO: Discovery Endpoint: http://localhost:8080/api/v1/
INFO: Rooms Resource:     http://localhost:8080/api/v1/rooms
INFO: Sensors Resource:   http://localhost:8080/api/v1/sensors
```

> **Note for Windows users without Maven on PATH:**
> ```powershell
> $env:PATH = "C:\maven\apache-maven-3.9.15\bin;" + $env:PATH
> mvn clean package
> ```

---

## API Endpoints Reference

| Method | Endpoint | Description | Success Code |
|--------|----------|-------------|--------------|
| GET | `/api/v1/` | Discovery — API metadata & HATEOAS links | 200 |
| GET | `/api/v1/rooms` | List all rooms | 200 |
| POST | `/api/v1/rooms` | Create a new room | 201 + Location |
| GET | `/api/v1/rooms/{roomId}` | Get a specific room | 200 |
| DELETE | `/api/v1/rooms/{roomId}` | Delete a room (blocked if sensors exist) | 204 |
| GET | `/api/v1/sensors` | List all sensors (supports `?type=` filter) | 200 |
| POST | `/api/v1/sensors` | Register a new sensor (validates roomId) | 201 + Location |
| GET | `/api/v1/sensors/{sensorId}` | Get a specific sensor | 200 |
| DELETE | `/api/v1/sensors/{sensorId}` | Remove a sensor | 204 |
| GET | `/api/v1/sensors/{sensorId}/readings` | Get all readings for a sensor | 200 |
| POST | `/api/v1/sensors/{sensorId}/readings` | Record a new reading (blocked if MAINTENANCE) | 201 + Location |

**Error Codes:**

| Code | Meaning | When Triggered |
|------|---------|----------------|
| 400 | Bad Request | Missing required fields (e.g., no `id` or `roomId`) |
| 403 | Forbidden | POST reading to a MAINTENANCE/OFFLINE sensor |
| 404 | Not Found | Room or sensor ID does not exist |
| 409 | Conflict | Duplicate ID, or deleting room with active sensors |
| 415 | Unsupported Media Type | Wrong `Content-Type` (not `application/json`) |
| 422 | Unprocessable Entity | `roomId` in sensor body references a non-existent room |
| 500 | Internal Server Error | Unexpected runtime error (global safety net) |

---

## Sample curl Commands

### 1. Discovery Endpoint
```bash
curl -X GET http://localhost:8080/api/v1/
```

### 2. Create a Room (POST — shows 201 + Location header)
```bash
curl -v -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LIB-301","name":"Library Quiet Study","capacity":50}'
```

### 3. List All Rooms
```bash
curl -X GET http://localhost:8080/api/v1/rooms
```

### 4. Get a Specific Room by ID
```bash
curl -X GET http://localhost:8080/api/v1/rooms/LIB-301
```

### 5. Register a Sensor (valid roomId)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-001","type":"Temperature","status":"ACTIVE","currentValue":21.5,"roomId":"LIB-301"}'
```

### 6. Register a Sensor with INVALID roomId (demonstrates 422)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-999","type":"Temperature","status":"ACTIVE","roomId":"NONEXISTENT"}'
```

### 7. Filter Sensors by Type
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=Temperature"
```

### 8. Post a Sensor Reading (updates sensor currentValue as side effect)
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":23.7}'
```

### 9. Get Reading History for a Sensor
```bash
curl -X GET http://localhost:8080/api/v1/sensors/TEMP-001/readings
```

### 10. Register MAINTENANCE sensor, then try to POST a reading (demonstrates 403)
```bash
# Step 1: Register the sensor in MAINTENANCE state
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"CO2-001","type":"CO2","status":"MAINTENANCE","roomId":"LIB-301"}'

# Step 2: Attempt a reading — returns 403 Forbidden
curl -X POST http://localhost:8080/api/v1/sensors/CO2-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":500}'
```

### 11. Attempt to delete a room with sensors (demonstrates 409)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

### 12. Delete a room safely (delete sensors first)
```bash
curl -X DELETE http://localhost:8080/api/v1/sensors/TEMP-001
curl -X DELETE http://localhost:8080/api/v1/sensors/CO2-001
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

---

## Conceptual Report — Question Answers

---

### Part 1.1 — JAX-RS Resource Lifecycle & In-Memory Data Synchronization

**Question:** Explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures.

**Answer:**

The JAX-RS specification mandates a **request-scoped lifecycle** for resource classes by default. This means the runtime (Jersey, in this implementation) creates a **brand new instance** of each resource class — `RoomResource`, `SensorResource`, etc. — for every single incoming HTTP request. The object is constructed, the relevant method is invoked, the response is sent, and the object is then discarded for garbage collection.

This design is intentional and beneficial for request isolation: any state specific to one request (e.g., path parameters stored as instance variables) cannot bleed into a subsequent request. However, it presents a fundamental challenge for in-memory persistence.

**The Critical Implication:**

If application data (rooms, sensors) were stored as instance variables inside resource classes, they would be created fresh and lost on every request:

```java
// WRONG — data destroyed after every request
@Path("/rooms")
public class RoomResource {
    private Map<String, Room> rooms = new HashMap<>(); // ← reset per-request!
}
```

**Our Solution — Static DataStore Singleton:**

All shared state is held in `DataStore`, a utility class with `static` fields. Static members belong to the Class itself (loaded once per JVM), not to any instance, so they persist for the entire application lifetime regardless of how many `RoomResource` objects are created and destroyed:

```java
public class DataStore {
    private static final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    // ...
}
```

**Why `ConcurrentHashMap`, not `HashMap`:**

The embedded Grizzly HTTP server uses a thread pool. Multiple requests can execute simultaneously on different threads, all accessing the same static `DataStore`. A plain `HashMap` is not thread-safe: concurrent reads and writes can cause `ConcurrentModificationException` or silent data corruption (lost updates, partial state visibility).

`ConcurrentHashMap` provides thread safety through **lock striping**: the map is divided into independent segments (16 by default), allowing reads with zero locking and writes with per-segment locks. This is dramatically faster than a fully synchronized `Collections.synchronizedMap()` wrapper under concurrent load — appropriate for a campus API that could serve thousands of sensor devices simultaneously.

For mutable `List<SensorReading>` values inside the map, we additionally use `synchronized` blocks on the list reference during write operations to prevent concurrent list corruption.

---

### Part 1.2 — HATEOAS and the Value of Hypermedia

**Question:** Why is the provision of "Hypermedia" (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?

**Answer:**

HATEOAS — **Hypermedia as the Engine of Application State** — is Level 3 of Leonard Richardson's REST Maturity Model and the defining quality that Roy Fielding identified as the characteristic of a truly RESTful architecture.

The core idea: **the server embeds navigational links within its responses**, enabling clients to discover available operations and resources dynamically, rather than relying on external documentation.

**Example from our Discovery Endpoint (`GET /api/v1/`):**
```json
{
  "resources": {
    "rooms": {
      "href": "http://localhost:8080/api/v1/rooms",
      "methods": "GET, POST, GET/{id}, DELETE/{id}"
    },
    "sensors": {
      "href": "http://localhost:8080/api/v1/sensors",
      "methods": "GET, GET?type={type}, POST, GET/{id}"
    }
  }
}
```

**Benefits over Static Documentation:**

| Dimension | Static Documentation | HATEOAS |
|---|---|---|
| **Discoverability** | Developer must read external docs before making any call | A single call to `/api/v1/` reveals the full API surface area |
| **Coupling** | Client hardcodes server URLs — changing `/rooms` to `/campus-rooms` breaks all clients | Client follows links; URL changes are transparent |
| **Accuracy** | Docs become stale as the API evolves | Links in responses always reflect current server state |
| **Exploration** | Requires Swagger UI or Postman collections | Any HTTP client (even a browser) can navigate the API |
| **Error Reduction** | Typos or version drift cause cryptic 404s | Valid links are always provided by the server |

In a campus infrastructure context, HATEOAS is particularly valuable: as new sensor types or building wings are added, client systems that follow links rather than hardcoding paths require zero code changes to remain functional.

---

### Part 2.1 — Full Objects vs. ID-Only Returns in Collection Responses

**Question:** When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client-side processing.

**Answer:**

**Returning ID-Only:**

- **Advantage:** Minimal payload — a list of 200 room IDs might be 3KB, versus 20KB for full objects.
- **Disadvantage:** Forces the N+1 problem. A client rendering a table of room names and capacities must make one initial request for IDs and then one `GET /rooms/{id}` per room. For 200 rooms, that is 201 HTTP round trips. On a wireless campus network with 50ms latency, this adds 10+ seconds of waiting before any data renders.
- **Use case:** Appropriate when clients only need to present a selection list (e.g., a dropdown of room IDs).

**Returning Full Objects (our implementation):**

- **Advantage:** A single request delivers everything — names, capacities, and sensor IDs. The client renders immediately without follow-up calls.
- **Disadvantage:** Slightly larger payload per request. However, modern HTTP/1.1 gzip compression makes JSON payloads of this size (typically 100–200 bytes per Room) negligible on any modern network.
- **Use case:** Superior for dashboard-style views where all fields are needed immediately, which is the primary use case for a campus facilities manager.

**Design Decision — Our Choice:**

We return full `Room` objects for the `GET /rooms` endpoint. The `Room` POJO is compact (4 fields), HTTP compression handles bandwidth efficiently, and the operational benefit of a single API call versus potentially hundreds far outweighs the marginal payload increase. The `sensorIds` list also gives clients immediate visibility of room occupancy status without additional requests.

---

### Part 2.2 — Idempotency of the DELETE Operation

**Question:** Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.

**Answer:**

Yes, `DELETE /api/v1/rooms/{roomId}` is **correctly idempotent** in this implementation, consistent with RFC 9110.

**Definition:** A method is idempotent if "the intended effect on the server of multiple identical requests is the same as the effect for a single such request."

**Sequence of Events:**

| Request # | Server State Before | Response | Server State After |
|-----------|--------------------|-----------|--------------------|
| 1st DELETE | Room `LIB-301` exists, no sensors | **204 No Content** | Room removed from DataStore |
| 2nd DELETE | Room `LIB-301` does not exist | **404 Not Found** | DataStore unchanged |
| 3rd DELETE | Room `LIB-301` does not exist | **404 Not Found** | DataStore unchanged |

**Key Insight:** Idempotency is about **server-side state**, not about receiving an identical HTTP status code. The critical observation is that after the first successful DELETE, the state of the server is "room does not exist." Each subsequent identical request leaves the server in that same state — room still does not exist. The server's state is therefore identical after one, two, or twenty identical DELETE requests. This satisfies the idempotency requirement.

**The 409 Edge Case:**

If the room has sensors, the DELETE returns 409 Conflict and performs no state change. Multiple 409 responses are also idempotent — the room still exists with its sensors after any number of blocked attempts.

---

### Part 3.1 — `@Consumes(APPLICATION_JSON)` and Content-Type Mismatch

**Question:** Explain the technical consequences if a client attempts to send data in a different format, such as `text/plain` or `application/xml`. How does JAX-RS handle this mismatch?

**Answer:**

When a resource method is annotated with `@Consumes(MediaType.APPLICATION_JSON)`, the JAX-RS runtime performs **content-type negotiation** as part of the request dispatching process — before the method body is ever executed.

**Mechanism:**

1. The client sends a `POST` request with `Content-Type: text/plain` (or `application/xml`).
2. Jersey reads the `Content-Type` header and compares it against all candidate resource methods' `@Consumes` annotations.
3. Finding no method that accepts `text/plain` for the given path, Jersey does **not invoke any resource method**.
4. Jersey automatically generates and returns **`HTTP 415 Unsupported Media Type`**.

This happens entirely within the JAX-RS framework layer — the application developer needs no custom code to handle this case.

**Why This Matters:**

This mechanism prevents several classes of bugs and vulnerabilities:
- **Deserialization errors:** Jackson cannot parse `text/plain` as JSON; allowing the call to proceed would throw an unchecked exception.
- **Injection prevention:** Processing raw text as if it were JSON could expose parsing vulnerabilities.
- **Contract clarity:** Clients immediately understand the exact data format requirement from the error response.

**The `@Produces` counterpart:**

Similarly, `@Produces(MediaType.APPLICATION_JSON)` governs response format negotiation via the `Accept` header. If a client sends `Accept: application/xml`, Jersey returns `HTTP 406 Not Acceptable`.

---

### Part 3.2 — `@QueryParam` vs. Path Parameters for Filtering

**Question:** Contrast the `@QueryParam` approach with an alternative design where the type is part of the URL path (e.g., `/api/v1/sensors/type/CO2`). Why is the query parameter approach generally considered superior for filtering and searching collections?

**Answer:**

| Design Aspect | `@QueryParam` → `/sensors?type=CO2` | Path Segment → `/sensors/type/CO2` |
|---|---|---|
| **REST Semantics** | Correct: the *resource* is `sensors`; the query modifies the *view* | Incorrect: implies `type/CO2` is a distinct nested resource |
| **Optionality** | Naturally optional; omit it to get all sensors | Requires a special-case endpoint for no filter |
| **Multiple Filters** | `?type=CO2&status=ACTIVE` — trivial | `/sensors/type/CO2/status/ACTIVE` — ugly and order-dependent |
| **Bookmarkability** | Clean, shareable: `sensors?type=CO2` | Awkward nesting |
| **HTTP Caching** | CDN cache keyed on path+query; filter variations handled correctly | Creates potentially infinite fake resource hierarchy |
| **Client Expectations** | Follows REST convention globally understood by developers | Non-standard, requires documentation to understand |

**Fundamental REST Principle:**

URLs identify **resources**. Query parameters describe **how to view or interact with** that resource. The collection `/api/v1/sensors` is the resource. Filtering is a view transformation, not a separate resource — exactly what query parameters are designed for.

**Our Implementation:**

```java
@GET
public Response getSensors(@QueryParam("type") String type) {
    if (type != null && !type.trim().isEmpty()) {
        return Response.ok(sensors filtered by type).build();
    }
    return Response.ok(all sensors).build();
}
```

When `?type=` is absent, all sensors are returned. When present, the collection is filtered — the same resource, different view. This single method handles both cases elegantly.

---

### Part 4.1 — The Sub-Resource Locator Pattern

**Question:** Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs?

**Answer:**

The Sub-Resource Locator is a JAX-RS mechanism where a resource method, annotated with `@Path` but **no HTTP method annotation** (`@GET`, `@POST`, etc.), returns an instance of another class. Jersey then inspects that returned object's annotations for further routing.

**In our implementation:**

```java
// In SensorResource.java
@Path("/{sensorId}/readings")
public SensorReadingResource getReadingsLocator(@PathParam("sensorId") String sensorId) {
    return new SensorReadingResource(sensorId);  // Delegate to specialist class
}
```

Jersey routes `POST /api/v1/sensors/TEMP-001/readings` by:
1. Matching `/sensors/TEMP-001` to `SensorResource`
2. Recognizing `readings` as a locator path → calling `getReadingsLocator("TEMP-001")`
3. Routing `POST /` on the returned `SensorReadingResource` object to its `@POST addReading()` method

**Architectural Benefits:**

**1. Single Responsibility Principle:**  
`SensorResource` manages sensor identity (create, find, delete sensors). `SensorReadingResource` manages reading history (append and retrieve readings). Neither class grows uncontrollably as features are added.

**2. Bounded Class Size:**  
A monolithic approach where all paths (`/sensors`, `/sensors/{id}`, `/sensors/{id}/readings`, `/sensors/{id}/readings/{rid}`) live in one class would quickly exceed 500+ lines, making it unmaintainable. Sub-resource locators keep each class focused and comprehensible.

**3. Independent Testability:**  
`SensorReadingResource` can be unit-tested in isolation by constructing it with a mock `sensorId` — no need to instantiate the entire `SensorResource` hierarchy.

**4. Physical Domain Mapping:**  
The code structure mirrors the campus hierarchy: Campus → Room → Sensor → Reading. This makes the architecture self-documenting and intuitive for new team members.

**5. Extensibility:**  
Future additions (e.g., `/sensors/{id}/alerts`, `/sensors/{id}/calibrations`) can each be new sub-resource classes — zero changes to the existing sensor or reading logic.

---

### Part 4.2 — Sensor Reading Side Effect (currentValue Update)

The `POST /api/v1/sensors/{sensorId}/readings` endpoint implements the required side effect: after successfully persisting a new `SensorReading`, it immediately updates the parent `Sensor` object's `currentValue` field:

```java
// In SensorReadingResource.addReading()
sensor.setCurrentValue(reading.getValue());
```

This ensures that a `GET /api/v1/sensors/{sensorId}` always reflects the most recent measurement without requiring a separate update call. The design is intentional: `currentValue` acts as a "live summary" always consistent with the latest reading event.

---

### Part 5.1 — HTTP 422 vs. 404 for Missing References in JSON Payloads

**Question:** Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?

**Answer:**

This distinction comes down to **what the status code is describing**: the URL or the payload content.

**HTTP 404 Not Found** means: the resource identified by the **request URL** (`/api/v1/sensors`) does not exist — a routing-level failure.

**HTTP 422 Unprocessable Entity** means: the server received the request, understood its content type, the JSON syntax is valid, but the **semantic meaning of the payload** contains an error that prevents processing.

**The Sensor Registration Scenario:**

When a client POSTs `{"id":"TEMP-001", "roomId":"GHOST-ROOM"}`:
- The URL `/api/v1/sensors` is perfectly valid ✓
- The `Content-Type: application/json` is correct ✓  
- The JSON syntax is valid ✓
- BUT `roomId: "GHOST-ROOM"` references a non-existent room ✗

Returning 404 would mislead the developer: they might assume the sensors endpoint itself doesn't exist, when in fact the sensors endpoint works perfectly — the `roomId` value in the body is the problem.

Returning 422 precisely communicates: "Your request reached the server, was understood, and the JSON parsed fine — but a value in your payload is semantically invalid." This guides the client to inspect the body content, not the URL.

**Response Body from our LinkedResourceNotFoundExceptionMapper:**
```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Cannot register sensor: roomId 'GHOST-ROOM' does not reference an existing room.",
  "hint": "Use GET /api/v1/rooms to see all valid rooms."
}
```

The `hint` field further demonstrates the API's commitment to developer experience, providing immediate actionable guidance.

---

### Part 5.2/5.3 — Custom ExceptionMapper Implementations

Our API implements three targeted ExceptionMappers that intercept domain-specific errors and convert them to meaningful HTTP responses:

| Exception | HTTP Code | Scenario |
|-----------|-----------|----------|
| `RoomNotEmptyException` | **409 Conflict** | DELETE attempted on room with sensors |
| `LinkedResourceNotFoundException` | **422 Unprocessable Entity** | POST sensor with non-existent roomId |
| `SensorUnavailableException` | **403 Forbidden** | POST reading to MAINTENANCE/OFFLINE sensor |

Each mapper:
1. Logs a WARNING server-side with the specific error message
2. Returns a structured JSON body with `status`, `error`, `message`, `hint`, and `timestamp` fields
3. Never exposes internal class names, stack traces, or implementation details

---

### Part 5.4 — Cybersecurity Risks of Exposing Stack Traces

**Question:** From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather?

**Answer:**

Exposing raw Java stack traces constitutes **Information Disclosure**, classified under OWASP A05:2021 (Security Misconfiguration) and CWE-209 (Generation of Error Message Containing Sensitive Information). A stack trace exposes:

**1. Internal File Paths and Package Structure:**
```
at com.smartcampus.store.DataStore.getOrCreateReadings(DataStore.java:103)
```
This reveals the exact package hierarchy, class names, and line numbers — a roadmap for understanding the application's internal architecture that attackers use to identify high-value targets.

**2. Framework and Library Versions:**
```
at org.glassfish.jersey.server.ServerRuntime$1.run(ServerRuntime.java:253)
at org.glassfish.grizzly.http.server.HttpServer.start(HttpServer.java:256)
```
Framework version strings allow attackers to cross-reference the **National Vulnerability Database (NVD)** for known CVEs affecting those exact versions (e.g., Jersey 2.39.1, Grizzly 2.4.4).

**3. Logic Flaws and Null Vulnerabilities:**
A `NullPointerException` at a specific method reveals that the code does not null-check certain inputs — a candidate for deliberate null-injection attacks to trigger undefined behavior.

**4. Business Rule Exposure:**
Method names like `validateSensorForRoom` or `checkRoomCapacity` in a trace reveal business rules that an attacker can systematically probe or attempt to bypass.

**5. Technology Fingerprinting:**
In competitive or adversarial scenarios, knowing the exact technology stack enables targeted attacks, social engineering, and supply-chain attacks on identified dependencies.

**Our Defense — `GlobalExceptionMapper`:**

The `GlobalExceptionMapper<Throwable>` catches all unexpected exceptions and returns only:
```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred. Please contact the system administrator.",
  "timestamp": 1776707477273
}
```

The full stack trace is logged server-side using `java.util.logging.Logger` at `SEVERE` level — visible to authorized developers in server logs, but never sent to the client. This implements the "least privilege" principle in error reporting: external parties receive only what they need (acknowledgement of failure), while internal observers receive full diagnostic detail.

---

### Part 5.5 — JAX-RS Filters for Cross-Cutting Concerns

**Question:** Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting `Logger.info()` statements inside every single resource method?

**Answer:**

A **cross-cutting concern** is functionality required uniformly across many components but belonging to none of them as core business logic. Logging, security, rate limiting, and CORS are classic examples.

**The Manual Logging Problem:**

With N resource methods (we have 10+), manual logging requires:
- N `Logger.info("Request received for...")` calls at method entry
- N `Logger.info("Response sent with status...")` calls at method exit
- Every future resource method requires the same boilerplate
- Forgetting to add logging to a new method silently creates observability gaps

**JAX-RS Filter Advantages:**

**1. DRY — Don't Repeat Yourself:**  
Our `LoggingFilter` automatically applies to 100% of requests and responses without touching a single resource method. Adding 10 more endpoints = zero additional logging code.

**2. Guaranteed Coverage:**  
Filters execute at the framework level, before and after the resource method. Even if a method throws an exception, the response filter still executes — logging the final status code.

**3. Separation of Concerns:**  
Resource methods contain only business logic. `RoomResource.createRoom()` creates rooms — it doesn't log, validate headers, add CORS headers, or check rate limits. Each component has one reason to change (SRP).

**4. Centralized Configuration:**  
Changing the log format (e.g., adding correlation IDs for distributed tracing, or response time in milliseconds) requires editing one class:

```java
// Single change, API-wide effect:
LOGGER.info(String.format("[REQUEST]  %-6s %s  (correlation-id: %s)",
        method, uri, UUID.randomUUID()));
```

**5. Testability:**  
Resource classes can be unit-tested without the filter. In production, the filter is registered. In tests, it can be excluded. Manual logging is always active in tests, polluting test output.

This mirrors the **AOP — Aspect-Oriented Programming** philosophy: the logging "aspect" weaves around the business logic at defined join points (before request, after response), keeping the core code immaculate.
