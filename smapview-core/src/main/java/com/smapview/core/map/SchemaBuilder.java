package com.smapview.core.map;

import java.util.Stack;

import com.smapview.core.map.NodeType.FolderField;
import com.smapview.core.map.NodeType.LinkField;
import com.smapview.core.map.NodeType.KeyField;
import com.smapview.core.map.NodeType.JoinField;
import com.smapview.core.map.NodeType.ValueField;
import com.smapview.core.map.NodeType.ValueType;
import com.smapview.core.util.RegularPattern;

public class SchemaBuilder {

	static RegularPattern FOLDER_SPEC_PATTERN = new RegularPattern("(-*)/([a-z][a-zA-Z0-9]+)");
	
	final NodePool pool; 
	
	final MapSchema schema;
	
	Stack<NodeType> context = new Stack<>();
	
	public SchemaBuilder(NodePool pool, MapSchema schema) {
		this.pool = pool;
		this.schema = schema;
	}

	/**
	 * Selects a node type, creating it if it does not exist.
	 * 
	 * @param typeName    Existing or new type name.
	 * @param folderSpec  Specifies the node container folder for this type.
	 * 
	 * @throws SchemaBuildException 
	 */
	public SchemaBuilder type(String typeName, String folderSpec) throws SchemaBuildException {
		String groups[] = FOLDER_SPEC_PATTERN.getGroups(folderSpec);
		int depth = groups[0].length();
		String folderName = groups[1];
		if (depth == 0) {
			context.clear();
			context.add(new NodeType(schema, typeName, folderName, newFolderAlias()));
		}
		else {
			if (context.size() < depth) throw new SchemaBuildException(
					String.format("No level-%s type selected", depth));
			while (context.size() != depth) context.pop();
			FolderField field = new FolderField(getLastType(), folderName,
					newLinkId(), typeName, newFolderAlias());
			context.add(new NodeType(schema, typeName, field));
		}
		return this;
	}
	
	NodeType getLastType() throws SchemaBuildException {
		if (context.isEmpty()) throw new SchemaBuildException("No type selected");
		else return context.peek();
	}

	public SchemaBuilder value(String name, ValueType valueType) throws SchemaBuildException {
		new ValueField(getLastType(), name, valueType, false);
		return this;
	}

	public SchemaBuilder list(String name, ValueType valueType) throws SchemaBuildException {
		new ValueField(getLastType(), name, valueType, true);
		return this;
	}

	public SchemaBuilder key(String name) throws SchemaBuildException {
		new KeyField(getLastType(), name, newKeyId());
		return this;
	}

	public SchemaBuilder link(String linkName, String targetType) throws SchemaBuildException {
		new LinkField(getLastType(), linkName, newLinkId(), targetType);
		return this;
	}

	public SchemaBuilder join(String linkName, String joinKey) throws SchemaBuildException {
		new JoinField(getLastType(), linkName, newLinkId(), joinKey);
		return this;
	}
	
	short newLinkId() {
		// TODO complete this
		return 0;
	}

	short newKeyId() {
		// TODO complete this
		return 0;
	}

	String newFolderAlias() {
		// TODO complete this
		return null;
	}
	
	/**
	 * Completes this builder, checking all schema links.
	 */
	public void build() throws SchemaBuildException {
		schema.build();
	}
	
}
