package com.smapview.view;

import java.util.regex.Pattern;

import jakarta.json.JsonObject;

public class FieldType {
	
	enum Kind {
		
		SCALAR,
		
		OBJECT, 
		
		INTERFACE,
		
		UNION,
		
		ENUM,
		
		INPUT_OBJECT,
		
		LIST,
		
		NON_NULL;
		
	}
	
	static final Pattern IGNORED_NAME = Pattern.compile(".*AggregateResult");

	final JsonObject schemaType;
	
	final Kind kind;
	
	final FieldType ofType;
	
	final String typeName;
	
	FieldType(JsonObject schemaType, ViewBuilder builder) {
		this.schemaType = schemaType;
		this.kind = Kind.valueOf(schemaType.getString("kind"));
		switch (kind) {
		case LIST:
		case NON_NULL:
			this.typeName = null;
			this.ofType = builder.getType(schemaType
					.getJsonObject("ofType").getString("name"));
			break;
		default:
			this.typeName = schemaType.getString("name");
			this.ofType = null;
			break;
		}
	}
	
	@Override
	public String toString() {
		switch (kind) {
		case LIST:
			return "[" + ofType.toString() + "]";
		case NON_NULL:
			return ofType.toString() + "!";
		default:
			return typeName;
		}		
	}
	
	public FieldType getBaseType() {
		FieldType baseType = this;
		while (baseType.ofType != null) baseType = baseType.ofType;
		return baseType;
	}

	public String getTypeName() {
		return typeName;
	}
	
	boolean isVisible() {
		switch (kind) {
		case LIST:
		case NON_NULL:
			return ofType.isVisible();
		case ENUM:
		case SCALAR:
			return true;
		case INPUT_OBJECT:
			return false;
		default:
			return ! IGNORED_NAME.matcher(typeName).matches();
		}
	}
		
}
