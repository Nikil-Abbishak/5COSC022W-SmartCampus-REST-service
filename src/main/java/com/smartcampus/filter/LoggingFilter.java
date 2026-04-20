package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * LoggingFilter.java — API Observability Filter (Part 5.5)
 *
 * Implements both ContainerRequestFilter and ContainerResponseFilter in a
 * single class, providing end-to-end request/response observability.
 *
 * This class is registered in SmartCampusApplication.getClasses() and
 * executes for every single HTTP request/response pair — without any
 * modification to the resource methods themselves.
 *
 * ============================================================
 * WHY FILTERS FOR CROSS-CUTTING CONCERNS? (Report Question 5.5)
 * ============================================================
 *
 * A cross-cutting concern is functionality required uniformly across many
 * components but not belonging to any single component's business logic.
 * Logging is the canonical example. Using JAX-RS filters provides:
 *
 * 1. DRY (Don't Repeat Yourself):
 *    With N resource methods, inserting Logger.info() manually requires
 *    2N additions (one at entry, one at exit). A single filter class handles
 *    ALL of them automatically. Adding a new resource method = zero logging code.
 *
 * 2. Consistency:
 *    Manual logging risks inconsistent format (debug vs info, missing fields).
 *    The filter enforces a uniform log pattern across the entire API surface.
 *
 * 3. Separation of Concerns:
 *    Resource methods should contain business logic only. Mixing logging in
 *    violates the Single Responsibility Principle and complicates unit testing.
 *    Filters are "outside" the resource, keeping it clean.
 *
 * 4. Centralized Control:
 *    Adding a correlation ID, timing metrics, or security audit trail requires
 *    editing ONE filter class — not hunting through dozens of resource methods.
 *
 * 5. Non-Invasive:
 *    Resource classes are entirely unaware of logging. They work identically
 *    in test environments where the filter might be excluded.
 *
 * This mirrors the Aspect-Oriented Programming (AOP) philosophy: logging,
 * security, and rate-limiting are "aspects" that weave around business logic.
 *
 * Log Format:
 *   [REQUEST]  {METHOD} {URI}
 *   [RESPONSE] {METHOD} {URI} → HTTP {status}
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    /**
     * Called BEFORE the request is dispatched to the resource method.
     * Logs the HTTP method and full request URI.
     *
     * @param requestContext  Provides access to method, URI, headers, and entity.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOGGER.info(String.format("[REQUEST]  %-6s %s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri()));
    }

    /**
     * Called AFTER the resource method has executed and a response is being sent.
     * Logs the method, URI, and the final HTTP status code.
     *
     * The response context gives us the status code without interfering with
     * the response body — the filter is transparent to both the resource and client.
     *
     * @param requestContext   Access to the original request details.
     * @param responseContext  Access to the outgoing response status and headers.
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        LOGGER.info(String.format("[RESPONSE] %-6s %s → HTTP %d",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri(),
                responseContext.getStatus()));
    }
}
