package com.smapview.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import com.smapview.view.LinkStrategy.JoinRole;
import com.smapview.view.NodeField.Modifier;
import com.smapview.view.NodeField.ValueType;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

class NodeType {

	static final Pattern IGNORED_TYPENAME = Pattern.compile(".*AggregateResult");
	
	final String typeName;
		
	final List<NodeField> fields;
	
	final boolean isInterface;
	
	final List<NodeType> possibleTypes;
		
	final List<JoinRole> joinRoles = new ArrayList<JoinRole>(8);
	
	GraphFieldSet fieldSet;

	NodeType(View view, JsonObject schemaType) {
		List<NodeField> fields = new ArrayList<>();
		for (JsonValue value : schemaType.getJsonArray("fields")) {
			JsonObject field = (JsonObject)value;
			String name = field.getString("name");
			JsonObject fieldType = field.getJsonObject("type");
			String fieldTypeName = fieldType.getString("name", null);
			if (fieldTypeName == null) {
				JsonObject ofType = fieldType.getJsonObject("ofType");
				fieldTypeName = ofType.getString("name");
			}
			String kind = fieldType.getString("kind");
			if (IGNORED_TYPENAME.matcher(fieldTypeName).matches()) continue;
			ValueType valueType = "ID".equals(fieldTypeName)? ValueType.ID
					: "String".equals(fieldTypeName)? ValueType.STRING
					: "DateTime".equals(fieldTypeName)? ValueType.DATE_TIME
							: ValueType.NODE;
			Modifier modifier = "NON_NULL".equals(kind)? Modifier.MANDATORY
					: "LIST".equals(kind)? Modifier.LIST : null;
			NodeField nodeField = valueType==ValueType.NODE? 
					new NodeField(this, name, fieldTypeName, modifier)
					: new NodeField(this, name, valueType, modifier);
			if ("ID".equals(fieldTypeName)) nodeField.add(FieldTag.ID); 
			fields.add(nodeField);
		}
		this.typeName = schemaType.getString("name");
		this.fields = Collections.unmodifiableList(fields);
		this.isInterface = schemaType.getString("kind", "").equals("INTERFACE");
		this.possibleTypes = listPossibleTypes(view, schemaType);
	}
	
	static private List<NodeType> listPossibleTypes(View view, JsonObject schemaType) {
		JsonArray types = schemaType.getJsonArray("possibleTypes");
		List<NodeType> result = new ArrayList<>();
		for (JsonValue value : types) {
			String ptypeName = ((JsonObject)value).getString("name");
			result.add(view.mapNodeType(ptypeName));
		}
		return Collections.unmodifiableList(result);
	}
		
	NodeField findField(String fieldName) {
		return fields.stream().filter(f -> f.fieldName.equals(fieldName))
				.findFirst().orElse(null);
	}

	NodeField getField(String fieldName) {
		NodeField result = findField(fieldName);
		if (result != null) return result;
		else throw new IllegalArgumentException(
				"Unknown field: " + typeName + "." + fieldName);
	}

	NodeField findFieldWith(FieldTag tag) {
		return fields.stream().filter(f -> f.has(tag)).findFirst().orElse(null);
	}

	NodeField getFieldWith(FieldTag tag) {
		NodeField field = findFieldWith(tag);
		if (field != null) return field;
		else throw new IllegalArgumentException("Missing "+tag+" field for "+typeName);
	}

	List<NodeField> findFieldsWith(FieldTag tag) {
		return fields.stream().filter(f -> f.has(tag)).toList();
	}
	
	@Override
	public String toString() {
		return typeName;
	}
	
	/**
	 * Tells if a specified type can be used to create a node of this type.
	 * 
	 * @param type  The specified type to test against.
	 * <p>
	 * @return True if this type is identical to the specified type and is not an interface,
	 *         or if this type refers to the specified type as one of its possible types.
	 */
	public boolean canBeCreatedWith(NodeType type) {
		return (this == type && !isInterface) 
				|| possibleTypes.contains(type);
	}
	
	String toFieldName() {
		return typeName.substring(0, 1).toLowerCase() + typeName.substring(1);
	}

	String toQueryName() {
		return "query" + typeName;
	}

	String toAddName() {
		return "add" + typeName;
	}

	String toGetName() {
		return "get" + typeName;
	}

	String toDeleteName() {
		return "delete" + typeName;
	}
			
	public NodeField getIdField() {
		return fieldSet.idField;
	}

	public NodeField getPointerField() {
		return fieldSet.pointerField;
	}

	public NodeField getTimestampField() {
		return fieldSet.timestampField;
	}

}
