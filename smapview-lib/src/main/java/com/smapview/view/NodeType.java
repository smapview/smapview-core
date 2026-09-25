package com.smapview.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.smapview.view.JoinStrategy.JoinRole;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

class NodeType {

	static final Pattern NAME = Pattern.compile("[A-Z][a-zA-Z_]*");

	static final Pattern IGNORED_NAME = Pattern.compile(".*AggregateResult");
	
	final String typeName;
		
	final Map<String,NodeField> fields;
	
	final boolean isInterface;
	
	final List<NodeType> possibleTypes;
		
	private final List<JoinRole> joinRoles = new ArrayList<JoinRole>(8);
	
	GraphFieldSet fieldSet;

	NodeType(View view, JsonObject schemaType) {
		Map<String,NodeField> fields = new HashMap<>(32);
		for (JsonValue value : schemaType.getJsonArray("fields")) {
			JsonObject field = (JsonObject)value;
			String name = field.getString("name");
			if (!NodeField.NAME.matcher(name).matches()) {
				throw new IllegalArgumentException("Invalid type name: "+name);
			}
			JsonObject fieldType = field.getJsonObject("type");
			String schemaTypeName = fieldType.getString("name", null);
			if (schemaTypeName == null) {
				JsonObject ofType = fieldType.getJsonObject("ofType");
				schemaTypeName = ofType.getString("name");
			}
			String kind = fieldType.getString("kind");
			if (IGNORED_NAME.matcher(schemaTypeName).matches()) continue;
			FieldTag typeTag = "ID".equals(schemaTypeName)? FieldTag.ID
					: "String".equals(schemaTypeName)? FieldTag.STRING
							: "DateTime".equals(schemaTypeName)? FieldTag.DATE_TIME
									: null;
			FieldTag modifier = "NON_NULL".equals(kind)? FieldTag.MANDATORY
					: "LIST".equals(kind)? FieldTag.LIST : null;
			NodeField nodeField = new NodeField(this, name, schemaTypeName, 
					typeTag, modifier);
			fields.put(nodeField.fieldName, nodeField);
		}
		this.typeName = schemaType.getString("name");
		this.fields = Collections.unmodifiableMap(fields);
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

	String toUpdateName() {
		return "update" + typeName;
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
	
	public NodeField getField(String fieldName) {
		return fields.get(fieldName);
	}
	
	void add(JoinRole role) {
		joinRoles.add(role);
		if (isInterface) {
			for (NodeType ptype : possibleTypes) {
				ptype.joinRoles.add(role);
			}
		}
	}
	
	Iterable<JoinRole> getJoinRoles() {
		return joinRoles;
	}

}
