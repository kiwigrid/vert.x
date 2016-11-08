package org.vertx.java.platform.configuration;

/**
 * This interface is the one to be implemented if you want to create your own configuration loader.
 * It will receive the path to the configuration and has to return a json string which is the configuration
 * correctly loaded.
 */
public interface ConfigurationLoader {
	
	String load(String configFilePath);
}
