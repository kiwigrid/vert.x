package org.vertx.java.platform.impl.cli;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.configuration.ConfigurationLoader;
import org.vertx.java.platform.impl.Args;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class StarterTest {
	
	private Starter starter = new Starter();
	
	@Test(expected = FileNotFoundException.class)
	public void loadConfigurationJsonThrowsFileNotFoundWithDefaultLoaderIfFileNotExist() throws Exception {
		starter.loadConfigurationJson("iDoNotExist", null);
	}
	
	@Test
	public void loadConfigurationJsonReturnsFileContentWithDefaultLoader() throws Exception {
		String filePath = "startertest/well-formed.json";
		String fullPath = getFullPath(filePath);

		JsonObject loadedContent = starter.loadConfigurationJson(fullPath, null);

		JsonObject expectedContent = new JsonObject(loadFile(fullPath));

		assertEquals("There was some discrepancy between the file content and returned from load method",
				expectedContent.toString(),
				loadedContent.toString());
	}
	
	@Test
	public void loadConfigurationJsonCallsCustomLoaderIfDefined() throws Exception {
		String fullPath = "notImportantPath";
		JsonObject loadedContent = starter.loadConfigurationJson(fullPath, "org.vertx.java.platform.impl.cli.StarterTest$ConfigLoaderStub");
		
		String result = loadedContent.getString("status");
		
		assertEquals("", "success", result);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void loadConfigurationThrowsIllegalArgumentExceptionIfCustomLoaderDoesNotExist() throws Exception {
		String fullPath = "notImportantPath";
		starter.loadConfigurationJson(fullPath, "classLoaderThatDoesNotExist");
	}

	@Test(expected = IllegalArgumentException.class)
	public void loadConfigurationThrowsIllegalArgumentExceptionIfCustomLoaderDoesNotImplementInterface() throws Exception {
		String fullPath = "notImportantPath";
		JsonObject loadedContent = starter.loadConfigurationJson(fullPath, "org.vertx.java.platform.impl.cli.StarterTest$ConfigLoaderStubThatDoesNotImplementInterface");

		String result = loadedContent.getString("status");

		assertEquals("", "success", result);
	}

	@Test(expected = IllegalArgumentException.class)
	public void loadConfigurationThrowsIllegalArgumentExceptionIfCustomLoaderIsNotInstantiable() throws Exception {
		String fullPath = "notImportantPath";
		JsonObject loadedContent = starter.loadConfigurationJson(fullPath, "org.vertx.java.platform.impl.cli.StarterTest$ConfigLoaderStubThatIsNotInstantiable");

		String result = loadedContent.getString("status");

		assertEquals("", "success", result);
	}

	private String getFullPath(String filePath) throws URISyntaxException {
		return getClass().getClassLoader().getResource(filePath).getPath();
	}

	private String loadFile(String fullPath) throws IOException, URISyntaxException {
		return new String(Files.readAllBytes(Paths.get(fullPath)));
	}
	
	public static class ConfigLoaderStub implements ConfigurationLoader{

		@Override
		public JsonObject load(String configFilePath) throws Exception {
			return new JsonObject("{\"status\":\"success\"}");
		}
	}
	
	public static class ConfigLoaderStubThatDoesNotImplementInterface{}

	private class ConfigLoaderStubThatIsNotInstantiable implements ConfigurationLoader{

		@Override
		public JsonObject load(String configFilePath) throws Exception {
			return new JsonObject("{\"status\":\"success\"}");
		}
	}
}
