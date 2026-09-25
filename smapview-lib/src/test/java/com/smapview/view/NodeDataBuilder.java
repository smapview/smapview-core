package com.smapview.view;

import java.util.Stack;

public class NodeDataBuilder {

	final View view;
	
	private final Stack<NodeData> stack = new Stack<>();

	private NodeDataBuilder(View view) {
		this.view = view;
	}
	
	static NodeDataBuilder newBuilder(View view) {
		return new NodeDataBuilder(view);
	}
	
	NodeDataBuilder set(String fieldName, String... values) throws GraphBuilderException {
		NodeType type = stack.peek().nodeType;
		NodeField field = type.getField(fieldName);
		if (field == null || ! field.has(FieldTag.STRING)) {
			throw new IllegalArgumentException("Invalid value field: " + fieldName);
		}
		else if (field.isList()) stack.peek().unsafeSet(field, values);
		else stack.peek().unsafeSet(field, values[0]);
		return this;
	}

	NodeDataBuilder addRoot(String typeName) throws GraphBuilderException {
		NodeType type = view.getNodeType(typeName);
		NodeData root = new NodeData(type);
		stack.push(root);
		return this;
	}

	NodeDataBuilder add(String pathField, String typeName) throws GraphBuilderException {
		NodeType type = view.getNodeType(typeName);
		NodeField path = get().nodeType.getField(pathField);
		if (path == null || ! path.isPath()) {
			throw new IllegalArgumentException("Invalid path field: " + pathField);
		}
		NodeData child = new NodeData(type);
		child.parentPath = path;
		get().add(path, child);
		stack.push(child);
		return this;
	}
	
	NodeDataBuilder endNode() {
		stack.pop();
		return this;
	}
	
	NodeData get() {
		return stack.peek();
	}

}
