package com.groceryerp.api;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS application bootstrap. The single {@code @ApplicationPath} sets the
 * base path for every REST resource, replacing the programmatic Jetty
 * ServletContextHandler/ServletHolder registration that used to live in
 * ApiServer.java. All endpoints remain under {@code /api} exactly as before.
 *
 * Resource and provider classes are discovered automatically by the container
 * via CDI + JAX-RS annotation scanning — no explicit getClasses() needed.
 */
@ApplicationPath("/api")
public class JaxRsApplication extends Application {
}
