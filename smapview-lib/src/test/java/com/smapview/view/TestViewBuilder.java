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

public class TestViewBuilder {

	static final String DEFAULT_URL = "http://localhost:7280";
	
	final String baseUrl;
	
	private TestViewBuilder() {
		this.baseUrl = DEFAULT_URL;
	}
	
	public static TestViewBuilder newBuilder() {
		return new TestViewBuilder();
	}
	
	public TestView build() throws ViewRequestException {
		return new TestView(baseUrl);
	}

	public TestViewBuilder setSchema(String name) throws IOException, InterruptedException {
		post(baseUrl + "/alter", "{\"drop_all\": true}");
		Common.trace("Cleared schema and data at %s", baseUrl);
		post(baseUrl + "/admin/schema", Files.readString(getSchema(name)));
		Common.trace("Uploaded schema %s to %s", name, baseUrl);
		return this;
	}	
	
	Path getSchema(String name) {
		return Path.of("src/test/resources/graphql/schema-NAME.graphql"
				.replace("NAME", name));
	}
	
	public TestViewBuilder clearData() throws IOException, InterruptedException {
		post(baseUrl + "/alter", "{\"drop_op\": \"DATA\"}");
		Common.trace("Cleared data at %s", baseUrl);
		return this;
	}
	
	void post(String url, String json) throws IOException, InterruptedException {
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
		}
	}

}
