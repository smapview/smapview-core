package com.smapview.view;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

class GraphFieldSet {
	
	enum FieldRole {
		ID,
		PATH,
		POINTER,
		TIMESTAMP
	}
	
	final NodeType type;
	
	final NodeField idField;
	
	final NodeField pointerField;
	
	final NodeField timestampField;
	
	private final List<NodeField> baseFields;

	private final Map<NodeField,GraphFieldSet> paths = new HashMap<>(8);

	private final List<GraphFieldSet> fragments = new LinkedList<>();

	private final Map<String,FieldRole> fieldRoles = new HashMap<>(12);
	
	GraphFieldSet(NodeType type) throws GraphSchemaException {
		this.type = type;
		idField = findFieldWith(FieldTag.ID);
		pointerField = findFieldWith(FieldTag.POINTER);
		timestampField = findFieldWith(FieldTag.TIMESTAMP);
		baseFields = Arrays.asList(idField, pointerField, timestampField)
				.stream().filter(f -> f != null).toList();
		for (NodeField field : baseFields) addRole(field);
	}
	
	NodeField findFieldWith(FieldTag tag) {
		return type.fields.values().stream()
				.filter(f -> f.has(tag)).findFirst().orElse(null);
	}
		
	void addPath(NodeField pathField, GraphFieldSet pathFieldSet) throws GraphSchemaException {
		paths.put(pathField, pathFieldSet);
		addRole(pathField, FieldRole.PATH);
	}
	
	void addFragment(GraphFieldSet fragment) throws GraphSchemaException {
		fragments.add(fragment);
		for (Entry<String,FieldRole> e : fragment.fieldRoles.entrySet()) {
			addRole(e.getKey(), e.getValue());
		}
	}

	private void addRole(NodeField field) throws GraphSchemaException {
		if (field.isId()) addRole(field, FieldRole.ID);
		else if (field.isPointer()) addRole(field, FieldRole.POINTER);
		else if (field.isTimestamp()) addRole(field, FieldRole.TIMESTAMP);
		else if (field.isPath()) addRole(field, FieldRole.PATH);
		else throw new IllegalArgumentException();
	}

	private void addRole(NodeField field, FieldRole role) throws GraphSchemaException {
		addRole(field.fieldName, role);
	}
	
	private void addRole(String fieldName, FieldRole role) throws GraphSchemaException {
		FieldRole oldRole = fieldRoles.get(fieldName);
		if (oldRole != null) {
			if (role != oldRole) throw new GraphSchemaException(
					"Multiple roles for field " + fieldName + " in " + type);
		}
		else fieldRoles.put(fieldName, role);
	}

	FieldRole getFieldRole(String fieldName) {
		return fieldRoles.get(fieldName);
	}
		
	@Override
	public String toString() {
		StringWriter buffer = new StringWriter();
		PrintWriter printer = new PrintWriter(buffer);
		writeTo(printer);
		printer.flush();
		return buffer.toString();
	}

	void writeTo(PrintWriter writer) {
		writeTo(writer, null, 0);
	}

	private boolean writeTo(PrintWriter writer, GraphFieldSet asFragmentFrom, int itemPos) {
		int itemCount = 0;
		if (asFragmentFrom == null)	{
			if (itemPos > 0) writer.append(", ");
			writer.append("{ ");
			for (NodeField field : baseFields) {
				append(writer, field, itemCount++);
			}
			for (NodeField pathField : paths.keySet()) {
				append(writer, pathField, itemCount++);
			}
			for (GraphFieldSet fragment : fragments) {
				if (fragment.writeTo(writer, this, itemCount)) itemCount++;
			}
			writer.append(" }");
		}
		else {
			for (NodeField field : baseFields) {
				if (!asFragmentFrom.hasField(field.fieldName)) {
					if (itemCount == 0) {
						if (itemPos > 0) writer.append(", ");
						writer.format("... on %s { ", type.typeName);
					}
					append(writer, field, itemCount++);
				}
			}
			for (NodeField field : paths.keySet()) {
				if (!asFragmentFrom.hasField(field.fieldName)) {
					if (itemCount == 0) {
						if (itemPos > 0) writer.append(", ");
						writer.format("... on %s { ", type.typeName);
					}
					append(writer, field, itemCount++);
				}
			}
			if (itemCount > 0) writer.append(" }");
		}
		return itemCount > 0;
	}
	
	private void append(PrintWriter writer, NodeField field, int fieldPos) {
		if (fieldPos > 0) writer.append(", ");
		writer.append(field.fieldName);
		if (field.isPath()) {
			writer.append(" ");
			field.getValueNodeType().fieldSet.writeTo(writer);
		}
	}
	
	boolean hasField(String fieldName) {
		for (NodeField field : baseFields) {
			if (field.fieldName.equals(fieldName)) return true; 
		}
		for (NodeField field : paths.keySet()) {
			if (field.fieldName.equals(fieldName)) return true; 
		}
		return false;
	}
	
	Collection<GraphFieldSet> listFragments() {
		return fragments;
	}

	Collection<Entry<NodeField,GraphFieldSet>> listPaths() {
		return paths.entrySet();
	}

	Entry<NodeField,GraphFieldSet> getPath(String pathField) {
		for (Entry<NodeField,GraphFieldSet> e : paths.entrySet()) {
			if (e.getKey().fieldName.equals(pathField)) return e; 
		}
		for (GraphFieldSet fragment : fragments) {
			Entry<NodeField,GraphFieldSet> found = fragment.getPath(pathField);
			if (found != null) return found;
		}
		return null;
	}
	
	GraphFieldSet getPathFieldSet(String pathField) {
		Entry<NodeField,GraphFieldSet> path = getPath(pathField);
		return path != null? path.getValue() : null;
	}
	
	Map<String,FieldRole> listFieldRoles() {
		return Collections.unmodifiableMap(fieldRoles);
	}
	
	NodeField getDefaultPathTo(NodeType nodeType) {
		List<NodeField> list = paths.keySet().stream() 
				.filter(f -> f.isAssignableFrom(nodeType)).toList();
		switch (list.size()) {
		case 0: throw new IllegalArgumentException("No path to " + nodeType);
		case 1: return list.getFirst();
		default: throw new IllegalArgumentException("Multiple paths to " + nodeType);
		}
	}
}
