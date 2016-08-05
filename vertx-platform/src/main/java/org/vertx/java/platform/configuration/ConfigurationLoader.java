package org.vertx.java.platform.configuration;

import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.impl.Args;

/**
 * Load and process Vertx configuration
 */
public interface ConfigurationLoader {
	JsonObject loadConfiguration(Args args);
}
