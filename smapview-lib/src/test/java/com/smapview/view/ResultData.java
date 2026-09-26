package com.smapview.view;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

public class ResultData {

	static final Pattern RELATIVE_PATH = Pattern.compile("([a-z][a-zA-Z]+)\\.([a-z].*)");
	
	final JsonValue data;
	
	public ResultData(JsonObject data) {
		String queryKey = null;
		for (String key : data.keySet()) {
			if (key.startsWith("query")) {
				queryKey = key;
				break;
			}
		}
		if (queryKey == null) throw new IllegalArgumentException("Cannot find query key");
		this.data = data.get(queryKey);
	}
	
	public void checkValues(String jsonPath, String... values) {
		Set<String> expected = new HashSet<>(Arrays.asList(values));
		Set<String> collected = new HashSet<>();
		collectValues(data, jsonPath, collected);
		assertTrue(expected.equals(collected));
	}
	
	static void collectValues(JsonValue data, String jsonPath, Set<String> values) {
		if (data instanceof JsonArray) {
			for (JsonValue value : (JsonArray)data) collectValues(value, jsonPath, values);
		}
		else if (data instanceof JsonObject) {
			JsonObject obj = (JsonObject)data;
			Matcher m = RELATIVE_PATH.matcher(jsonPath);
			if (m.matches()) {
				collectValues(obj.get(m.group(1)), m.group(2), values);
			}
			else values.add(obj.getString(jsonPath));
		}
	}

}
