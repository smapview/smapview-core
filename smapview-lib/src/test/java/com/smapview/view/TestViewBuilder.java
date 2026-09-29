package com.smapview.view;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

public class TestViewBuilder extends ViewBuilder {

	static final String DEFAULT_URL = "http://localhost:7280";
	
	final String baseUrl;
	
	private TestViewBuilder(TestView view, String schema) 
			throws ViewRequestException 
	{
		super(view);
		this.baseUrl = DEFAULT_URL;
		post(baseUrl + "/alter", "{\"drop_all\": true}");
		Common.trace("Cleared schema and data at %s", baseUrl);
		post(baseUrl + "/admin/schema", schema);
		Common.trace("Uploaded schema to %s", baseUrl);
		init();
	}
	
	public static TestViewBuilder newWithSchema(String schemaName) throws ViewRequestException, IOException  
	{
		TestView view = new TestView(DEFAULT_URL);
		String schema = Files.readString(getSchema(schemaName));
		return new TestViewBuilder(view, schema);
	}
	
	public TestViewBuilder setSchema(String name) throws ViewRequestException, IOException {
		return this;
	}	
	
	static Path getSchema(String name) {
		return Path.of("src/test/resources/graphql/schema-NAME.graphql"
				.replace("NAME", name));
	}
	
	public TestViewBuilder clearData() throws ViewRequestException {
		post(baseUrl + "/alter", "{\"drop_op\": \"DATA\"}");
		Common.trace("Cleared data at %s", baseUrl);
		return this;
	}
	
	void post(String url, String json) throws ViewRequestException {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.timeout(Duration.ofSeconds(10))
				.POST(BodyPublishers.ofString(json))
				.build();
		try (InputStream response = HttpClient.newHttpClient()
				.send(request, BodyHandlers.ofInputStream()).body())
		{
			JsonValue result = Json.createReader(response).readValue();
			if (result instanceof JsonObject) {
				JsonObject obj = (JsonObject)result;
				if (obj.containsKey("errors") 
						|| ! obj.getJsonObject("data").getString("code").equals("Success")) 
				{
					throw new IllegalStateException("Operation failed: " + result);
				}
			}
			else {
				throw new IllegalStateException("Operation failed with status " + result);
			}
		} catch (IOException | InterruptedException e) {
			throw new ViewRequestException(e);
		}
	}

}
