package com.smapview.core.map;

import com.smapview.core.map.NodeType.FolderField;
import com.smapview.core.map.NodeType.LinkField;
import com.smapview.core.map.NodeType.TypeField;
import com.smapview.core.map.NodeType.ValueType;

public class SchemaBuilder {

	final NodePool pool; 
	
	final MapSchema schema;
	
	NodeType type;
	
	LinkField link;

	public SchemaBuilder(NodePool pool, MapSchema schema) {
		this.pool = pool;
		this.schema = schema;
	}

	/**
	 * Selects a node type, creating it if it does not exist.
	 * 
	 * @param typeName    Existing or new type name.
	 * @param folderName  Root folder or folder field name, for type creation.
	 * 
	 * @throws SchemaBuildException 
	 */
	public SchemaBuilder type(String typeName, String folderName) throws SchemaBuildException {
		type = schema.getType(typeName);
		if (type == null) {
			if (folderName == null) {
				new NodeType(schema, typeName);
			}
			else if (folderName.contains(".")) {
				TypeField field = schema.expectDeclaredField(folderName);
				if (field instanceof FolderField ff) {
					new NodeType(schema, typeName, ff);
				}
				else throw new SchemaBuildException("Not a folder field: "+field.name);
			}
			else {
				new NodeType(schema, typeName, folderName, pool.newFolderAlias());				
			}
		}
		return this;
	}

	private void checkTypeSelected() throws SchemaBuildException {
		if (type == null) throw new SchemaBuildException("No type selected");
	}

	public SchemaBuilder field(String name, ValueType baseType) throws SchemaBuildException {
		checkTypeSelected();
		new NodeType.ValueField(type, name, baseType, null);
		return this;
	}

	public SchemaBuilder list(String name, ValueType itemType) throws SchemaBuildException {
		checkTypeSelected();
		new NodeType.ValueField(type, name, ValueType.LIST, itemType);
		return this;
	}

	public SchemaBuilder link(String linkName, String targetType) throws SchemaBuildException {
		checkTypeSelected();
		link = new LinkField(type, linkName, pool.newLinkId(), targetType);
		return this;
	}

	private void checkLinkSelected() throws SchemaBuildException {
		if (link == null) throw new SchemaBuildException("No link or folder selected");
	}

	public SchemaBuilder folder(String folderName) throws SchemaBuildException {
		checkTypeSelected();
		// TODO complete this
		return this;
	}

	private void checkFolderSelected() throws SchemaBuildException {
		if (link == null || !(link instanceof FolderField)) {
			throw new SchemaBuildException("No folder selected");
		}
	}

}
