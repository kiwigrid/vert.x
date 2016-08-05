package org.vertx.java.platform.configuration;

import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.impl.Args;

/**
 * Default configuration loader that just loads a json file from disk
 */
public class JsonFileConfigurationLoader implements ConfigurationLoader {

	@Override
	public JsonObject loadConfiguration(Args args) {
		String configFile = args.map.get("-conf");
		return ConfigurationUtils.loadJsonFromFile(configFile);
	}
}
