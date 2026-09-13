package com.smapview.view;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class NodeData {

	final NodeType nodeType;
	
	String nodePointer;

	NodeInfo nodeInfo;	
	
	NodeType parentType;

	NodeField parentPath;
		
	private final Map<NodeField,Object> fieldValues = new HashMap<>(50);
	
	NodeData(NodeType nodeType) {
		this.nodeType = nodeType;
	}
	
	void set(NodeField field, Object value) throws GraphBuilderException {
		// check value type
		Class<?> expectedJavaType = null;
		if (value != null ) {
			switch (field.valueType) {
			case ID:
			case STRING:
				if (field.isList()) expectedJavaType = String[].class;
				else expectedJavaType = String.class;
				break;
			case DATE_TIME:
				expectedJavaType = Date.class;
				break;
			case NODE:
				throw new GraphBuilderException("Cannot set value on a link field");
			default:
				// all value types should be covered above
				throw new Error();
			}
			if (value.getClass() != expectedJavaType) {
				throw new GraphBuilderException("Invalid value type " + 
						toJavaTypeName(value.getClass()) +
						" for " + field + ", expected " + 
						toJavaTypeName(expectedJavaType));
			}
		}
		// check null vs mandatory
		if (field.isMandatory() && value == null) {
			throw new GraphBuilderException("Value cannot be null for " + field);
		}
		unsafeSet(field, value);
	}
	
	void unsafeSet(NodeField field, Object value) {
		fieldValues.put(field, value);;
	}
	
	private static String toJavaTypeName(Class<?> type) {
		return type.getName().replaceAll(".*\\.", "");
	}
		
	@SuppressWarnings("unchecked")
	<T> T unsafeGet(NodeField field) {
		return (T) fieldValues.get(field);
	}
		
	void writeTo(ViewRequest request) {
		// TODO uncomment below code and work on updates
//		if (nodeInfo.nodeUid != null) {
//			writer.write("id", nodeInfo.nodeUid);
//		}
		// reference to root node
		if (nodeInfo != null && nodeInfo.parentNode !=null 
				&& nodeInfo.parentNode.nodeId != null) 
		{
			request.startInputObject(parentPath.inverseField);
			request.append(parentType.getIdField(), nodeInfo.parentNode.nodeId);
			request.endInputObject();
		}
		for (Map.Entry<NodeField, Object> fval : fieldValues.entrySet()) {
			NodeField field = fval.getKey();
			if (field.fieldName.startsWith("_")) continue;
			else switch (field.valueType) {
			case ID:
			case STRING:
				if (field.isList()) {
					request.append(field, (String[])fval.getValue());
				}
				else {
					request.append(field, (String)fval.getValue());
				}
				break;
			case NODE:
				request.startInputObject(field);
				// case of a path field, always a list
				@SuppressWarnings("unchecked")
				List<NodeData> data = (List<NodeData>)fval.getValue();
				data.stream().forEach(n -> n.writeTo(request));
				request.endInputObject();
				break;
			case DATE_TIME:
				// TODO complete this
				break;
			}
		}
	}
	
	boolean hasNonNull(NodeField field) {
		return fieldValues.get(field) != null;
	}
	
	Iterable<NodeField> getFields() {
		return fieldValues.keySet();
	}
			
	void appendSubIds(StringBuffer buffer) {
		for (Map.Entry<NodeField, Object> fval : fieldValues.entrySet()) {
			if (fval.getKey().has(FieldTag.PATH)) {
				@SuppressWarnings("unchecked")
				List<NodeData> data = (List<NodeData>)fval.getValue();
				if (!data.isEmpty()) {
				}
			}
		}
	}
	
}
