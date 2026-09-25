package com.smapview.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class NodeData {
	
	static int ADD_PARENT_REF = 0x01;

	static int SKIP_ID = 0x02;

	static int SKIP_POINTER = 0x04;

	final NodeType nodeType;
	
	NodeInfo nodeInfo;	
	
	NodeType parentType;

	NodeField parentPath;
		
	private final Map<NodeField,Object> fieldValues = new HashMap<>(50);
	
	NodeData(NodeType nodeType) {
		this.nodeType = nodeType;
	}
	
	
	void set(NodeField field, Object value) throws GraphBuilderException {
		// check value type
		if (field == null || value == null) throw new NullPointerException();
		else if (! field.isValidInput(value)) {
			throw new GraphBuilderException("Invalid value for " +
					field.getGraphqlType() + " field " + field);
		}
		else unsafeSet(field, value);
	}
	
	void add(NodeField pathField, NodeData childData) {
		List<NodeData> childList = unsafeGet(pathField);
		if (childList == null) {
			childList = new ArrayList<>(32);
			unsafeSet(pathField, childList);
		}
		childList.add(childData);
	}

	void unsafeSet(NodeField field, Object value) {
		fieldValues.put(field, value);;
	}
			
	@SuppressWarnings("unchecked")
	<T> T unsafeGet(NodeField field) {
		return (T) fieldValues.get(field);
	}

	void writeTo(ViewRequest request) {
		writeTo(request, 0);
	}

	void writeTo(ViewRequest request, int options) {
		boolean addParentRef = (options & ADD_PARENT_REF) != 0;
		boolean skipId = (options & SKIP_ID) != 0;
		boolean skipPointer = (options & SKIP_POINTER) != 0;
		// write id or pointer field
		if (nodeInfo != null && nodeInfo.getNodeId() != null && ! skipId) {
			request.append(nodeType.getIdField(), nodeInfo.getNodeId());
		}
		else if (addParentRef && nodeInfo != null 
				&& nodeInfo.parentNode != null 
				&& nodeInfo.parentNode.getNodeId() != null) 
		{
			request.startInputObject(parentPath.getInverseField());
			request.append(parentType.getIdField(), nodeInfo.parentNode.getNodeId());
			request.endInputObject();
			request.append(nodeType.getPointerField(), getPointer());
		}
		else if (! skipPointer){
			request.append(nodeType.getPointerField(), getPointer());
		}
		// write other fields
		for (Map.Entry<NodeField, Object> fval : fieldValues.entrySet()) {
			NodeField field = fval.getKey();
			if (! field.hasAny(FieldTag.POINTER, FieldTag.LINK)) {
				field.writeValueTo(fval.getValue(), request);
			}
		}
	}
		
	boolean hasNonNull(NodeField field) {
		return fieldValues.get(field) != null;
	}
	
	Collection<NodeField> getFields() {
		return fieldValues.keySet();
	}
	
	boolean hasPointer() {
		return fieldValues.containsKey(nodeType.fieldSet.pointerField);
	}

	String getPointer() {
		return unsafeGet(nodeType.fieldSet.pointerField);
	}
	
	void checkMandatoryFields() throws GraphBuilderException {
		for (NodeField field : nodeType.fields.values()) {
			if (field.needsInput() && ! fieldValues.containsKey(field)) {
				throw new GraphBuilderException("Mandatory field not set: "+field);
			}
		}
	}

}
