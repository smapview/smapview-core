package com.smapview.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

class NodeData {
	
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
		for (NodeField field : nodeType.getFields()) {
			if (field.needsInput() && ! fieldValues.containsKey(field)) {
				throw new GraphBuilderException("Mandatory field not set: "+field);
			}
		}
	}
	
	String getNodeId() {
		return nodeInfo != null? nodeInfo.getNodeId() : null;
	}
	
	NodeField getIdField() {
		return nodeType.getIdField();
	}
	
	NodeField getPointerField() {
		return nodeType.getPointerField();
	}

	String getParentNodeId() {
		return nodeInfo != null && nodeInfo.parentNode != null? 
				nodeInfo.parentNode.getNodeId() : null;
	}

	NodeField getParentIdField() {
		return parentType != null? parentType.getIdField() : null; 
	}

	NodeField getInversePath() {
		return parentPath != null? parentPath.getInverseField() : null;
	}
	
	void forEachValue(Predicate<NodeField> ifTest, BiConsumer<NodeField,Object> thenDo) {
		fieldValues.entrySet().stream()
		.filter(e -> ifTest.test(e.getKey()))
		.forEach(e -> thenDo.accept(e.getKey(), e.getValue()));
	}
	
	@Override
	public String toString() {
		return "" + fieldValues;
	}
	
}
