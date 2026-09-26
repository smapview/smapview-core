package com.smapview.view;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;


public class TestView extends View {

	TestView(String dgraphHttpUrl) throws ViewRequestException {
		super(dgraphHttpUrl);
	}

	public ResultData execGraphQL(String query) throws IOException, InterruptedException {
		URI endpoint = this.graphqlEndpoint;
		String contentType = "application/graphql";
		HttpRequest request = HttpRequest.newBuilder().uri(endpoint)
				.timeout(Duration.ofSeconds(10))
				.header("Content-Type", contentType)
				.POST(BodyPublishers.ofString(query))
				.build();
		try (InputStream stream = client.send(request, BodyHandlers.ofInputStream()).body()) {
			JsonObject result = Json.createReader(stream).readObject();
			JsonArray errors = result.getJsonArray("errors");
			JsonObject data = result.getJsonObject("data");
			Common.trace("Got request result: %s", result);
			if (errors == null || errors.isEmpty()) return new ResultData(data);
			else throw new RuntimeException("Got response with error(s): " + errors);
		}
	}

}
