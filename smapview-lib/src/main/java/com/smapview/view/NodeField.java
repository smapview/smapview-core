package com.smapview.view;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import com.smapview.view.FieldType.Kind;

class NodeField {

	static final Pattern NAME = Pattern.compile("[a-z_][a-zA-Z_]*");
	
	static final Pattern QUALIFIED_NAME = 
			Pattern.compile("(" + NodeType.NAME + ")\\.(" + NAME + ")");
	
	final String fieldName;
	
	final NodeType declaringType;

	final FieldType fieldType;
	
	private Class<?> inputType;

	private int tags = 0;

	private NodeField inverseField;	
	
	private LinkStrategy linkStrategy;

	NodeField(NodeType type, String fieldName, FieldType fieldType) {
		if (!NAME.matcher(fieldName).matches()) {
			throw new IllegalArgumentException("Invalid type name: "+fieldName);
		}
		this.fieldName = fieldName;
		this.fieldType = fieldType;
		this.declaringType = type;
		String baseTypeName = fieldType.getBaseType().typeName;
		if ("ID".equals(baseTypeName)) add(FieldTag.ID);
		else if ("String".equals(baseTypeName)) add(FieldTag.STRING);
		else if ("DateTime".equals(baseTypeName)) add(FieldTag.DATE_TIME);
		else if (fieldType.getBaseType() instanceof EnumType) add(FieldTag.ENUM);
		if (fieldType.kind == Kind.NON_NULL) add(FieldTag.MANDATORY);
		else if (fieldType.kind == Kind.LIST) add(FieldTag.LIST);
	}
	
	private void add(FieldTag tag) {
		this.tags |= tag.value;
	}
	
	boolean has(FieldTag searchedTag) {
		return (tags & searchedTag.value) != 0;
	}

	boolean hasAny(FieldTag... searchedTags) {
		for (FieldTag tag : searchedTags) {
			if (has(tag)) return true;
		}
		return false;
	}

	boolean hasAll(FieldTag... searchedTags) {
		for (FieldTag tag : searchedTags) {
			if (! has(tag)) return false;
		}
		return true;
	}

	void markAsPath(NodeField inverseField, ViewBuilder builder) {
		if (hasAny(FieldTag.ID, FieldTag.STRING, FieldTag.DATE_TIME)
				|| ! has(FieldTag.LIST)) 
		{
			throwInvalidTypeFor("path");
		}
		else {
			checkValueNodeType();
			setInverse(inverseField, builder);
			add(FieldTag.PATH);
			if (declaringType.isAbstract()) {
				for (NodeType type : declaringType.getPossibleTypes()) {
					NodeField same = type.getField(fieldName);
					same.add(FieldTag.PATH);
					same.inverseField = inverseField;
				}
			}
		}
	}

	void markAsLink(LinkStrategy linkStrategy, NodeField inverseField, ViewBuilder builder) {
		if (hasAny(FieldTag.ID, FieldTag.STRING, FieldTag.DATE_TIME)) {
			throwInvalidTypeFor("link");
		}
		else {
			checkValueNodeType();
			this.linkStrategy = linkStrategy;
			setInverse(inverseField, builder);
			add(FieldTag.LINK);
			if (declaringType.isAbstract()) {
				for (NodeType type : declaringType.getPossibleTypes()) {
					NodeField same = type.getField(fieldName);
					same.linkStrategy = linkStrategy;
					same.inverseField = inverseField;
					same.add(FieldTag.LINK);
				}
			}
		}
	}

	private void setInverse(NodeField inverse, ViewBuilder builder) {
		if (fieldType.getBaseType() == inverse.declaringType 
				&& inverse.fieldType.getBaseType() == declaringType) 
		{
			this.inverseField = inverse;
			inverse.add(FieldTag.REVERSE);
			inverse.inverseField = this;
		}
		else {
			throw new IllegalArgumentException("Field types do not match declaring types");
		}
	}
	
	void markAsPointer() {
		NodeField existing = declaringType.findFieldWith(FieldTag.POINTER);
		if (existing != null) {
			throw new IllegalArgumentException(
					"Existing pointer field for " + declaringType.typeName + 
					": " + existing.fieldName);
		}
		else if (hasAny(FieldTag.STRING, FieldTag.ENUM) && has(FieldTag.MANDATORY)) {
			add(FieldTag.POINTER);
			if (declaringType.isAbstract()) {
				for (NodeType type : declaringType.getPossibleTypes()) {
					type.getField(fieldName).markAsPointer();
				}
			}
		}
		else throwInvalidTypeFor("pointer");
	}

	void markAsTimestamp() {
		if (has(FieldTag.DATE_TIME) && ! has(FieldTag.LIST)) {
			add(FieldTag.TIMESTAMP);
			if (declaringType.isAbstract()) {
				for (NodeType type : declaringType.getPossibleTypes()) {
					type.getField(fieldName).markAsTimestamp();
				}
			}
		}
		else throwInvalidTypeFor("timestamp");

	}
	
	void throwInvalidTypeFor(String role) {
		throw new IllegalArgumentException("Invalid type for " + role + 
				" field: " + getGraphqlType());
	}

	@Override
	public String toString() {
		if (declaringType == null) return fieldName;
		return declaringType.typeName + "." + fieldName;
	}
	
	boolean isMandatory() {
		return has(FieldTag.MANDATORY);
	}

	boolean isList() {
		return has(FieldTag.LIST);
	}

	boolean isId() {
		return has(FieldTag.ID);
	}

	boolean isLink() {
		return has(FieldTag.LINK);
	}

	boolean isPath() {
		return has(FieldTag.PATH);
	}

	boolean isReverse() {
		return has(FieldTag.REVERSE);
	}

	boolean isPointer() {
		return has(FieldTag.POINTER);
	}

	boolean isTimestamp() {
		return has(FieldTag.TIMESTAMP);
	}
	
	boolean needsInput() {
		return inputType != null && isMandatory();
	}

	boolean allowsInput() {
		return inputType != null;
	}

	boolean isValidInput(Object inputValue) {
		return inputValue != null && inputType != null
				&& inputType.isAssignableFrom(inputValue.getClass());
	}
	
	void prepareForInput() {
		inputType = has(FieldTag.DATE_TIME)? Date.class
				: has(FieldTag.STRING)? (has(FieldTag.LIST)? String[].class : String.class)
						: has(FieldTag.ENUM)? String.class
								: has(FieldTag.PATH)? List.class 
										: has(FieldTag.LINK)? JoinValue[].class : null;	
		if (inputType != null) {
			Common.trace("Prepared %s for input type %s", this, inputType);
		}
	}
	
	Class<?> getInputType() {
		return inputType;
	}
	
	String getGraphqlType() {
		return fieldType.toString();
	}

	NodeType getValueNodeType() {
		return (NodeType)fieldType.getBaseType();
	}

	NodeField getInverseField() {
		return inverseField;
	}
	
	boolean isAssignableFrom(NodeType type) {
		return getValueNodeType().isImplementedBy(type);
	}

	public LinkStrategy getLinkStrategy() {
		return linkStrategy;
	}

	void checkValueNodeType() {
		NodeType type = getValueNodeType();
		if (type.isAbstract() && ! type.hasPossibleTypes()) {
			throw new IllegalArgumentException("Field" + this +
					" value type " + type.typeName + " not implemented");
		}
	}

}
