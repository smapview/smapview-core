package com.smapview.view;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.json.JsonArray;

public class EnumType extends FieldType {

	List<String> values;
	
	EnumType(FieldType baseType, ViewBuilder builder) {
		super(baseType.schemaType, builder);
		JsonArray enumValues = schemaType.getJsonArray("enumValues");
		String[] values = new String[enumValues.size()];
		for (int i = 0; i < values.length; i++) {
			values[i] = enumValues.getJsonObject(i).getString("name");
		}
		this.values = Collections.unmodifiableList(Arrays.asList(values));
	}
	
	static boolean canCreateFrom(FieldType type) {
		switch (type.kind) {
		case ENUM:
			return true;
		default:
			return false;
		}
	}

	public List<String> getValues() {
		return values;
	}

}
