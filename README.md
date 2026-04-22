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

---

## Conceptual Report

**Module:** Client-Server Architecture (5COSC022C.2)  
**Module Lecturer:** Cassim Farook  
**Student Name:** S N A Priya  
**IIT Student ID:** 20240850  
**UOW Student ID:** W2152991  

### Abstract
In this report, the architect of a smart campus api based on jax rs and embedded server to provide high performance is described. The architecture makes use of thread safe memory data structures to hold state and hateoas in navigation of resources. Through the use of the sub resource locators and specific exception mappers to certain http status codes, the system has been enabled to nest very deep and very powerful error handling. Observability filters also guarantee that the service is scalable and resilient in general.

### Acknowledgement
I want to express my sincere thanks to Mr. Cassim Farook and the teaching assistants for their dedicated guidance during the Client-Server Architectures module. The comprehensive lectures and practical lab sessions were vital in providing the expertise required to develop this project. I truly appreciate the continuous support and insights shared by the faculty, which were instrumental in the successful completion and implementation of this coursework assignment.

### Abbreviation Table

| Abbreviation | Meaning |
|---|---|
| api | application programming interface |
| jax rs | jakarta restful web services |
| rest | representational state transfer |
| hateoas | hypermedia as the engine of application state |
| http | hypertext transfer protocol |
| json | javascript object notation |
| xml | extensible markup language |
| crud | create read update delete |
| url | uniform resource locator |

### Part 1 - Service Architecture & Setup

#### 1.1 JAX-RS resource lifecycle and in-memory synchronisation
**Question:** Explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronise your in-memory data structures (maps/lists) to prevent data loss or race conditions.

The default of a JAX-RS resource class is request-scoped. This would imply that the runtime would initialize a new resource instance with every new incoming HTTP request unless there was a special setting. The advantage of such a model is that it is isolated: request-specific state is not accidentally leaked between two calls by the same client.

This significant implication is that application data that has to survive across requests should not be stored in resource instances fields. Having a RoomResource and a SensorResource maintain her rooms and sensors within just normal instance variables would ensure that it would be recreated and discarded each request. The persistent in-memory state must thus reside outside the resource objects per se.

Here, the shared data is stored in the central DataStore that is supported by the ConcurrentHashMap collections. That design is similar to the request-scoped lifecycle since the resource objects are still lightweight request handlers with the actual application state being stored in a structure with a lifetime that is equal to that of the server process. It also does not cause a loss of data between requests.

Concurrency point is as well significant. The built-in HTTP server is able to handle multiple requests simultaneously, hence a number of threads might attempt creating or updating rooms, sensors or sensor readings simultaneously. The thread-safe collections minimize chances of race conditions, or inconsistent reads or lost updates. But one also has to consider what is outside the map per se: Assuming that the values which are contained within the map itself are not fixed values, but are lists of values instead, changes to the values contained within them still must be exercised with caution that the overall state is not disturbed.

#### 1.2 Hypermedia and HATEOAS
**Question:** Why is the provision of hypermedia links and navigation within responses considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?

Hypermedia is regarded as the attribute of a mature designs of RESTful as it renders the API as self-descriptive. The server does not impose on clients the common notion of wholly depending on external documentation, instead the server provides links and hints within its responses thus allowing its client to know what resources are available as well as what is to be done next.

This is important as it will minimize the coupling between the client and server. A discovery response links a client to need to hard-code as many assumptions on URL structures. Running an API may mean that the server can still recommend the client the new links without knowing how to handle this situation, instead of each developer having to remember the change but trying to diagnose how this impacts the server.

Hypermedia offers live and up to date guidance when compared to the conventional documentation (using a paper format). Documentation may also get out of date, incomplete or not in line with the deployed service. Comparatively, links that are given by the running API will always be a reflection of the implementation at that time. To the client developers this enhances the discoverability, less trial and error, and safer integration.

In the case of this Smart Campus API, version information, contact information and links to the main collections (i.e. rooms and sensors) could be made available via a discovery endpoint at GET /api/v1. That provides developers with a starting point and simplifies the exploration of the service with common HTTP tools.

### Part 2 - Room Management

#### 2.1 Returning room IDs only versus full room objects
**Question:** When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client-side processing.

There is lower bandwidth usage because the payload returned when room IDs are only returned is smaller hence it is the most bandwidth-efficient. It may come in handy in cases where an actual client just requires a fairly small list of identifiers, like a basic selector, or a follow-up look up workflow.

The trade-off is that, responses (ID only) compel more work on the part of the client. In the event the client requires details of the room name, capacity or the interconnected sensors, he or she has to place extra demands with each room. Scaled up, that exposes the age-old N+1 request security issue of a single request to get the IDs, and a multitude of requests to get the details. Although individually tiny, the aggregate response time and the amount of orchestration cost on the client can grow significantly greater than responding with more substantive objects in the first place.

Full room returning can make the collection response larger, but tends to decrease overall network traffic and can make the data more user-friendly by dashboard-friendly clients since the data required to display it is already available. In this coursework, a room object is comparatively lightweight and thus the compensation in the payload increase is not so much whereas the compensation in the more convenientness is high.

That is why the more robust design option of this API is reverted full room objects. It provides facilities managers and automated clients with the most useful data instantly with a single request, and nevertheless maintains the response structure straightforward and effective.

| Approach | Main advantage | Main drawback | Best fit |
|---|---|---|---|
| IDs only | Small response body | Requires extra requests for details | Simple lookups or selectors |
| Full room objects | One request gives usable data immediately | Slightly larger payload | Dashboards, management views, automation |

#### 2.2 Idempotency of DELETE
**Question:** Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.

Yes. DELETE is an idempotent operation as reusing a request cannot continue to alter the state of the server once the initial successful deletion has occurred.

When there is a room and also none of the sensors are assigned on it, the initial DELETE request will remove it and a successful response will be 204 No Content. In case the client subsequently resends the identical DELETE request, then the room has since been removed, hence the API leads to 404 Not Found. Though the code of status is varied, the key issue is that the status is not changed after the initial successful delete: the room is still non-existent.

This is precisely the meaning of idempotency in HTTP. Not all repeated requests need to come back with the same status code. It is a promise that the desired property change in resource state corresponds with the desired property change in resource state after one request as after multiple requests, which are identical.

The reasoning would work similarly when the deletion is prevented as sensors remain fixed to the room. Then when the request returns 409 Conflict the room still exists. Re-sending the same DELETE will not cause any changes to the room, thus the server state will not change.

### Part 3 - Sensor Operations & Linking

#### 3.1 Consequences of @Consumes(MediaType.APPLICATION_JSON)
**Question:** We explicitly use the @Consumes(MediaType.APPLICATION_JSON) annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as text/plain or application/xml. How does JAX-RS handle this mismatch?

When an annotation to a resource method is annotated with @Consumes(MediaType.APPLICATION_JSON), it is stating that only request bodies in the form of JSON are accepted. JAX-RS verifies a Content-Type header on the way into the method body.

In case a client submits the request as text/plain, application/xml, or other media type that is not supported, the runtime will not direct the payload to the method which is only supported by JSON. Rather it denies the request during the framework stage and sends back a 415 Unsupported Media Type. Stated another way, the alignment of mismatch is processed prior to the business logic.

This behaviour is essential since it upholds the API contract in a clean manner. The server is not required to make guesses about how to decode the payload, or the application code to do separate checks in all incorrect formats. It also avoids the unnecessary deserialisation errors since the registered JSON provider is required to accept only the content intended to be in the form of a JSON.

As a distinction, useful here is that a content type error is a result of a wrong Content-Type but malformed JSON with the correct Content-Type would be a different problem. The second one usually gets to the message body reader and stops when the content is to be negotiated instead of when there is a parsing problem.

#### 3.2 Why @QueryParam is better for filtering collections
**Question:** You implemented this filtering using @QueryParam. Contrast this with an alternative design where the type is part of the URL path, for example /api/v1/sensors/type/CO2. Why is the query parameter approach generally considered superior for filtering and searching collections?

Query parameters are usually preferable due to the absence of resource creation with filtering; it generates a new view of an existing collection of resources. The admissions remain /api/v1/sensors. The type value merely reduces the set of results which is brought about by that collection.

The hierarchy in which the resource is taken to be stiffer than it is by using a path like /api/v1/sensors/type/CO2. It renders the filter to resemble a nested resource instead of optional search condition. It gets uncomfortable the moment additional filters are inserted, since the path becomes lengthier and stiffer.

Query parameters are much better scaled also. A query, like /api/v1/sensors?type=CO2&status=ACTIVE, can be read and extended easily, and order-independent. And it is more stiff, less traditional and less maintainable to do the same thing with path segments.

To clients, the query strings are likely to be the expected format to search, sort, and filter endpoints of collections. The application of the @QueryParam makes the API thus consistent with usual REST conventions, as well as makes the endpoint easier to use.

| Design | Meaning | Strength | Weakness |
|---|---|---|---|
| `/sensors?type=CO2` | Filtered view of the sensors collection | Optional and easy to combine with other filters | Slightly longer URL when many filters exist |
| `/sensors/type/CO2` | Looks like a nested resource path | Readable for one fixed case | Poor fit for optional or multiple filters |

### Part 4 - Deep Nesting with Sub-Resources

#### 4.1 Architectural value of the Sub-Resource Locator pattern
**Question:** Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path in one massive controller class?

Sub-Resource Locator pattern enhances the API design of large API by isolating concerns based on resource context. Here, sensorlevel operation is taken care of by a SensorResource with a sensorreadingresource looking after sensor history that is owned by a certain sensor. The real domain model relates to that division, and simplifies the code.

Delegation to another class eliminates the risk of the main resource constituting a big controller that manages all the nested paths, validation rules, and responsiveness figures. With the API size, maintaining all the paths in a single class would complicate the code to read, debug, and introduce more bugs. Nested behaviour can be broken down into specific resource classes to maintain the focus of each class and make it manageable.

It is also a better way of improving maintainability. Logic associated with readings e.g. can develop separately of sensor registration logic. A history reader programmer does not need to have to drown in irrelevant room or sensor strategies to locate the code. That decreases interconnectedness within the codebase.

Lastly the pattern is generalizable. With nested resources having been modeled neatly, the process of adding further sub-resources towards the end e.g. alerts, calibration records or maintenance logs becomes significantly simpler without stressing out the parent controller resource.

### Part 5 - Advanced Error Handling, Exception Mapping & Logging

#### 5.2 Why 422 Unprocessable Entity is more accurate than 404
**Question:** Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?

The HTTP 404 Not Found is majorly concerned with the request target which is determined by the URL. It informs the client that the resource that was being sought out could not be located at the path that was requested.

A 422 Unprocessable Entity response explains an alternate scenario: the request has reached a valid endpoint, its syntax is correct, and the server has known how to process the request correctly, but one of the values included in it is semantically invalid. It occurs in this coursework when a sensor with an invalid roomId (that is, a non-existing sensor) is posted to /api/v1/sensors.

At that point it is obvious that the endpoint will exist, thus, 404 would not have been indicative. It is the relationship, which is encoded within the payload that is the actual problem and not the URL. A response of 422 shall inform the client that the request body was fine, but makes no logical sense.

This is why 422 will be the better option when it comes to linked-resource validation. It assists the client in diagnosing the correct problem in the quickest way and differentiates between payload errors and routing errors.

#### 5.4 Cybersecurity risks of exposing stack traces
**Question:** From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?

Raw Java stack traces are not meant to be seen by outside users since they provide them with technical information that is not meant to be told to outsiders. Rather than displaying a generic error screen, an attacker can get to understand the manner in which the application is compiled and in which areas it might be prone to attacks.

The stack trace may contain the names of packages, names of classes, and names of methods, names of source files, and even line numbers. That assists an attacker in mapping the internal layout on the application and comprehend where specified actions or verifications take place. It is also capable of revealing framework and library elements and this enables technology fingerprinting and facilitated searching known vulnerable spots in the disclosed stack.

Also, stack traces can provide a point of failure. That can indicate what inputs might not have been safely processed, what data might be a null object, or what assumptions within the code might be violated with specially crafted requests. That is to say, aside from being a diagnosing data to the developers, the trace can as well be reconnaissance data to attackers.

The less risky architecture would be to send out a controlled 500 Internal Server Error response in which a generic message is sent to the client with all the diagnostic information being logged over the server to allow authorised developers and administrators.

#### 5.5 Why filters are better for cross-cutting logging
**Question:** Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting Logger.info() statements inside every single resource method?

Logging should be stored in JAX-RS filters since logging is a cross-cutting, and not business logic. It is applicable to all the end points, whether the endpoint is responsible of managing rooms, sensors or readings.

With ContainerRequestFilter and ContainerResponseFilter, the latter has centralised the same. The logging rules are developed on a one time basis and then made to be uniform throughout the entire API. That prevents the monotony of using the same boilerplate in all the resource methods and will decrease the likelihood of a developer missing to log a new endpoint.

Reliability is also enhanced by filters. Since they run in the request-response framework, they are able to capture incoming requests and status code outgoing responses even in case a resource method raises an exception. Old fashioned logging within methods of the controllers is less noticeable and more difficult to maintain.

Most importantly, this division ensures that the resource classes stick to what they are meant to do: accomplish the domain behaviour of the API. The outcome is less code, reduced maintenance, and enhanced observability.
