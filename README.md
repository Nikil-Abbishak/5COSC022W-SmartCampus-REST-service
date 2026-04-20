# Smart Campus Sensor & Room Management API

Welcome to the Smart Campus Sensor & Room Management API! This project is a backend service designed to manage physical campus spaces and monitor IoT sensors deployed across a university campus.

## API Design Overview

The API is built using **pure JAX-RS (Jersey)** and runs on an embedded **Grizzly HTTP server**, meaning it is entirely self-contained without needing external application servers like Tomcat. For persistence, it relies on entirely thread-safe, in-memory data structures (`ConcurrentHashMap`) to bypass database requirements.

The architecture strictly adheres to RESTful principles:
- **Resource Orientation:** Clear physical decoupling by managing `rooms`, `sensors`, and historical `readings`.
- **HATEOAS Discovery:** The root endpoint (`/api/v1/`) provides hypermedia links to gracefully discover the API's capabilities.
- **Sub-Resource Locators:** Sensor readings (`/sensors/{id}/readings`) are routed cleanly using nested locators to separate concerns.
- **Defensive State Management:** The system safely rejects operations that violate business logic (e.g., throwing a `409 Conflict` if you try to delete a room containing active sensors, or returning `403 Forbidden` if you post readings to a sensor in maintenance). All errors are caught by custom JAX-RS exception mappers and returned as structured, secure JSON. 

## How to Build and Run the Server

To try out this API locally, follow these explicit step-by-step instructions.

**Prerequisites:** Ensure you have Java 11+ and Maven 3.6+ installed on your system.

### Step 1: Clone the repository
In your terminal, clone this project using git and navigate into the directory:
```bash
git clone https://github.com/Nikil-Abbishak/5COSC022W-SmartCampus-REST-service.git
cd 5COSC022W-SmartCampus-REST-service
```

### Step 2: Build the project
Use Maven to clean the directory and package the code into an executable fat-JAR:
```bash
mvn clean package
```
*Note: This might take a minute the first time as Maven downloads the required dependencies (such as Jersey and Grizzly).*

### Step 3: Launch the server
Once the `BUILD SUCCESS` message appears, start the server by running the compiled JAR from the `target/` directory:
```bash
java -jar target/SmartCampusAPI-1.0.0.jar
```
The Grizzly server will boot up and bind to port 8080. You should see a success message indicating the server is running. Leave this terminal window open to keep the server alive!

---

## Example Interactions (cURL)

Below are five sample cURL commands demonstrating successful, core interactions across the different parts of the API. You can safely run these in a secondary terminal window one by one.

**1. Create a Library study room:**
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LIB-301", "name":"Library Quiet Study", "capacity":50}'
```

**2. List all available rooms:**
```bash
curl -X GET http://localhost:8080/api/v1/rooms
```

**3. Register a new temperature sensor inside the room we just made:**
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-001", "type":"Temperature", "status":"ACTIVE", "roomId":"LIB-301"}'
```

**4. Check the metadata of our newly registered sensor:**
```bash
curl -X GET http://localhost:8080/api/v1/sensors/TEMP-001
```

**5. Post a new live reading to that sensor (updates the sensor's current value automatically!):**
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":23.7}'
```
