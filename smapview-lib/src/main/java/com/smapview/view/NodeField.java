package com.smapview.view;

import java.util.Arrays;

class NodeField {

	enum ValueType {
		ID,
		NODE,
		STRING,
		DATE_TIME
	}
	
	enum Modifier {
		LIST,
		MANDATORY
	}
		
	final String fieldName;
	
	final ValueType valueType;
	
	final String valueSchemaType;
	
	final Modifier modifier;
		
	final NodeType declaringType;
	
	NodeType valueNodeType;

	NodeField inverseField;

	FieldTag[] tags = {};

	NodeField(NodeType type, String fieldName, ValueType valueType, Modifier modifier) {
		this.fieldName = fieldName;
		this.valueType = valueType;
		this.valueSchemaType = null;
		this.modifier = modifier;
		this.declaringType = type;
	}

	NodeField(NodeType type, String fieldName, String valueSchemaType, Modifier modifier) {
		this.fieldName = fieldName;
		this.valueType = ValueType.NODE;
		this.valueSchemaType = valueSchemaType;
		this.modifier = modifier;
		this.declaringType = type;
	}

	void add(FieldTag tag) {
		tags = Arrays.copyOf(tags, tags.length+1);
		tags[tags.length-1] = tag;
	}
	
	boolean has(FieldTag searchedTag) {
		for (FieldTag tag : tags) {
			if (tag == searchedTag) return true;
		}
		return false;
	}

	boolean hasAny(FieldTag... searchedTags) {
		for (FieldTag tag : searchedTags) {
			if (has(tag)) return true;
		}
		return false;
	}

	@Override
	public String toString() {
		if (declaringType == null) return fieldName;
		return declaringType.typeName + "." + fieldName;
	}
	
	public boolean isList() {
		return modifier == Modifier.LIST;
	}

	public boolean isMandatory() {
		return modifier == Modifier.MANDATORY;
	}
	
}
