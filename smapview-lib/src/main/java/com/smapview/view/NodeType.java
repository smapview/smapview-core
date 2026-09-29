package com.smapview.view;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

class NodeType extends FieldType {

	static final Pattern NAME = Pattern.compile("[A-Z][a-zA-Z_]*");
	
	private final Map<String,NodeField> fields = new HashMap<>(32);
	
	private final List<NodeType> possibleTypes = new LinkedList<>();;
		
	private final List<JoinValueExpr> jvexList = new LinkedList<>();
	
	private boolean excluded = false;
	
	GraphFieldSet fieldSet;

	NodeType(FieldType baseType, ViewBuilder builder) {
		super(baseType.schemaType, builder);
		if (! NAME.matcher(typeName).matches()) {
			throw new IllegalArgumentException("Invalid node type name: " + typeName);
		}
	}
	
	static boolean canCreateFrom(FieldType type) {
		switch (type.kind) {
		case UNION:
		case INTERFACE:
		case OBJECT:
			return true;
		default:
			return false;
		}
	}
	
	boolean isAbstract() {
		return kind != Kind.OBJECT;
	}
	
	@Override
	boolean isVisible() {
		return super.isVisible() && ! excluded;
	}
	
	boolean hasFields() {
		return ! fields.isEmpty();
	}
	
	void exclude() {
		this.excluded = true;
	}
	
	void completeWith(ViewBuilder builder) {
		if (fields.isEmpty()) {
			Common.trace("Completing node type %s from %s", typeName, schemaType);
			for (JsonValue value : schemaType.getJsonArray("fields")) {
				JsonObject field = (JsonObject)value;
				String fieldName = field.getString("name");
				FieldType fieldType = new FieldType(field.getJsonObject("type"), builder);
				if (fieldType.typeName != null) {
					// reset to known type if it has a name
					fieldType = builder.getType(fieldType.typeName);
				}
				if (fieldType.isVisible()) {
					NodeField nodeField = new NodeField(this, fieldName, fieldType); 
					fields.put(nodeField.fieldName, nodeField);
					Common.trace("Added field %s with type %s", nodeField, fieldType);;
				}
				else {
					Common.trace("Skipped field %s.%s with type %s", 
							typeName, fieldName, fieldType);;
				}
			}
			for (JsonValue value : schemaType.getJsonArray("possibleTypes")) {
				possibleTypes.add(builder.getNodeType(((JsonObject)value).getString("name")));
			}
		}
	}
	
	void trimFields() {
		fields.values().stream()
		.filter(f -> ! f.fieldType.isVisible())
		.toList().stream().forEach(f -> fields.remove(f.fieldName));
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
	 * @return True if this type is identical to the specified type and is not abstract,
	 *         or if this type refers to the specified type as one of its possible types.
	 */
	public boolean isImplementedBy(NodeType type) {
		return (this == type && ! isAbstract()) || possibleTypes.contains(type);
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
	
	NodeField findFieldWith(FieldTag tag) {
		for (NodeField field : fields.values()) {
			if (field.has(tag)) return field;
		}
		return null;
	}

	void add(JoinValueExpr jvex) {
		jvexList.add(jvex);
		if (isAbstract()) {
			for (NodeType ptype : possibleTypes) {
				ptype.jvexList.add(jvex);
			}
		}
	}
	
	Iterable<JoinValueExpr> getJoinValueExprs() {
		return jvexList;
	}
	
	Iterable<NodeField> getFields() {
		return fields.values();
	}

	Iterable<NodeType> getPossibleTypes() {
		return possibleTypes;
	}
	
	boolean hasPossibleTypes() {
		return ! possibleTypes.isEmpty();
	}

}
