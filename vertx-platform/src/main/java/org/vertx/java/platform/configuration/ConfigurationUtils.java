package org.vertx.java.platform.configuration;

import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 *
 */
public class ConfigurationUtils {
	private static final Logger log = LoggerFactory.getLogger(ConfigurationUtils.class);

	/**
	 * Parse json from a file on the filesystem
	 * @param fileName file to parse
	 * @return parsed json, or null if any errors were encountered
	 */
	public static JsonObject loadJsonFromFile(String fileName) {
		JsonObject conf;

		if (fileName != null) {
			try (Scanner scanner = new Scanner(new File(fileName)).useDelimiter("\\A")){
				String sconf = scanner.next();
				try {
					conf = new JsonObject(sconf);
				} catch (DecodeException e) {
					log.error("Configuration file does not contain a valid JSON object");
					return null;
				}
			} catch (FileNotFoundException e) {
				log.error("Config file " + fileName + " does not exist");
				return null;
			}
		} else {
			conf = null;
		}
		return conf;
	}
}
