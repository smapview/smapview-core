package com.smapview.view;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

class NodeField {

	static final Pattern NAME = Pattern.compile("[a-z_][a-zA-Z_]*");
	
	static final Pattern QUALIFIED_NAME = 
			Pattern.compile("(" + NodeType.NAME + ")\\.(" + NAME + ")");

	private interface InputWriter {
		void writeTo(NodeField field, Object value, ViewRequest request);
	}
		
	private static final InputWriter STRING_WRITER = new InputWriter() {
		@Override
		public void writeTo(NodeField field, Object value, ViewRequest request) {
			request.append(field, (String)value);
		}
	};

	private static final InputWriter STRING_ARRAY_WRITER = new InputWriter() {
		@Override
		public void writeTo(NodeField field, Object value, ViewRequest request) {
			request.append(field, (String[])value);
		}
	};

	private static final InputWriter DATE_TIME_WRITER = new InputWriter() {
		@Override
		public void writeTo(NodeField field, Object value, ViewRequest request) {
			// TODO complete this
		}
	};

	private static final InputWriter NODE_DATA_WRITER = new InputWriter() {
		@Override
		public void writeTo(NodeField field, Object value, ViewRequest request) {
			request.startInputArray(field);
			// case of a path field, always a list
			@SuppressWarnings("unchecked")
			List<NodeData> list = (List<NodeData>)value;
			for (NodeData data : list) {
				request.startInputObject();
				data.writeTo(request);
				request.endInputObject();
			}
			request.endInputArray();
		}
	};

	
	final String fieldName;
	
	final NodeType declaringType;

	final private String schemaBaseType;
	
	private InputWriter inputWriter;
	
	private Class<?> inputType;

	private int tags = 0;

	private NodeType valueNodeType;

	private NodeField inverseField;	

	NodeField(NodeType type, String fieldName, String schemaBaseType, FieldTag... tags) {
		this.fieldName = fieldName;
		this.schemaBaseType = schemaBaseType;
		this.declaringType = type;
		for (FieldTag tag : tags) if (tag != null) add(tag);
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

	void markAsPath(NodeField inverseField, View view) {
		if (hasAny(FieldTag.ID, FieldTag.STRING, FieldTag.DATE_TIME)
				|| ! has(FieldTag.LIST)) 
		{
			throwInvalidTypeFor("path");
		}
		else {
			setInverse(inverseField, view);
			add(FieldTag.PATH);
			if (declaringType.isInterface) {
				for (NodeType type : declaringType.possibleTypes) {
					NodeField same = type.getField(fieldName);
					same.add(FieldTag.PATH);
					same.inverseField = inverseField;
					same.valueNodeType = valueNodeType;
				}
			}
		}
	}

	void markAsLink(NodeField inverseField, View view) {
		if (hasAny(FieldTag.ID, FieldTag.STRING, FieldTag.DATE_TIME)) {
			throwInvalidTypeFor("link");
		}
		else {
			setInverse(inverseField, view);
			add(FieldTag.LINK);
			if (declaringType.isInterface) {
				for (NodeType type : declaringType.possibleTypes) {
					NodeField same = type.getField(fieldName);
					same.add(FieldTag.LINK);
					same.inverseField = inverseField;
					same.valueNodeType = valueNodeType;
				}
			}
		}
	}

	private void setInverse(NodeField inverse, View view) {
		NodeType fromType = view.mapNodeType(inverse.schemaBaseType);
		NodeType toType = view.mapNodeType(schemaBaseType);
		if (toType == inverse.declaringType && fromType == declaringType) {
			this.inverseField = inverse;
			this.valueNodeType = toType;
			inverse.add(FieldTag.REVERSE);
			inverse.inverseField = this;
			inverse.valueNodeType = fromType;
		}
		else {
			Common.trace("Compared toType=%s fromType=%s declType=%s invDeclType=%s",
					fromType, toType, declaringType, inverse.declaringType);
			throw new IllegalArgumentException("Field types do not match declaring types");
		}
	}
	
	void markAsPointer() {
		if (hasAll(FieldTag.STRING, FieldTag.MANDATORY)) {
			add(FieldTag.POINTER);
			if (declaringType.isInterface) {
				for (NodeType type : declaringType.possibleTypes) {
					type.getField(fieldName).markAsPointer();
				}
			}
		}
		else throwInvalidTypeFor("pointer");
	}

	void markAsTimestamp() {
		if (has(FieldTag.DATE_TIME) && ! has(FieldTag.LIST)) {
			add(FieldTag.TIMESTAMP);
			if (declaringType.isInterface) {
				for (NodeType type : declaringType.possibleTypes) {
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

	void writeValueTo(Object inputValue, ViewRequest request) {
		inputWriter.writeTo(this, inputValue, request);
	}
	
	boolean needsInput() {
		return inputType != null && isMandatory();
	}

	boolean isValidInput(Object inputValue) {
		return inputValue != null && inputType != null
				&& inputType.isAssignableFrom(inputValue.getClass());
	}
	
	void prepareForInput() {
		inputType = has(FieldTag.DATE_TIME)? Date.class
				: hasAny(FieldTag.STRING, FieldTag.LINK)? 
						(has(FieldTag.LIST)? String[].class : String.class)
						: has(FieldTag.PATH)? List.class : null;	
		inputWriter = has(FieldTag.DATE_TIME)? DATE_TIME_WRITER 
				: has(FieldTag.STRING)? 
						(has(FieldTag.LIST)? STRING_ARRAY_WRITER : STRING_WRITER)
						: has(FieldTag.PATH)? NODE_DATA_WRITER : null;
		if (inputType != null) {
			Common.trace("Prepared %s for input type %s", this, inputType);
		}
	}
	
	String getGraphqlType() {
		return isList()? "[" + schemaBaseType + "]"
				: isMandatory()? schemaBaseType + "!"
						: schemaBaseType;
	}

	NodeType getValueNodeType() {
		return valueNodeType;
	}

	NodeField getInverseField() {
		return inverseField;
	}
	
	boolean isAssignableFrom(NodeType type) {
		return valueNodeType != null && valueNodeType.canBeCreatedWith(type);
	}

}
